package eu.toolchain.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An interface that defines a contract with a computation that could be asynchronous.
 * <p>
 * <h1>Thread Safety</h1>
 * <p>
 * All methods exposed in {@code CompletionStage} are fully <em>thread-safe</em>, guaranteeing that
 * interactions with the future are atomic.
 * <p>
 * <h1>States</h1>
 * <p>
 * A future has four states.
 * <ul>
 * <li><em>running</em>, which indicates that the future is currently active, and has not reached an
 * end-state.</li>
 * <li><em>completed</em>, which indicates that the computation was successful, and produced a
 * result.</li>
 * <li><em>failed</em>, which indicates that the computation failed through an exception, which can
 * be fetched for inspection.</li>
 * <li><em>cancelled</em>, which indicates that the computation was cancelled.</li>
 * </ul>
 * <p>
 * The last three states are characterized as <em>end states</em>, a future can only transition into
 * one of these, and when in an end-state will never go into another state. If a future is in and
 * end state it is considered <em>done</em>, as is indicated by the {@link #isDone()} method.
 * <p>
 * A stage is typically used in combination with future computations.
 *
 * @param <T> the type being provided by the future
 * @author udoprog
 */
public interface CompletionStage<T> {
  /**
   * If not already completed, cancel the future completion.
   * <p>
   * This will cause {@link #isDone()} to be {@code true}, and {@link #isCancelled()} to be
   * {@code true}.
   *
   * @return {@code true} if the future was cancelled by this call
   */
  boolean cancel();

  /**
   * Get the result of the future.
   *
   * @return the result of the computation
   * @throws ExecutionException if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   */
  T join() throws ExecutionException, InterruptedException;

  /**
   * Get the result of the future.
   *
   * @return the result of the computation
   * @throws ExecutionException if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   * @throws TimeoutException if the computation does not finish within the given timeout
   */
  T join(long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException;

  /**
   * Get the result of the future without blocking.
   *
   * @return the result of the computation
   * @throws IllegalStateException if the result is not available
   * @throws ExecutionException if the computation threw an exception
   * @throws CancellationException if the computation was cancelled
   */
  T joinNow() throws ExecutionException;

  /**
   * Indicates if the stage is done
   *
   * @return {@code true} if the future is completed, failed, or cancelled
   */
  boolean isDone();

  /**
   * Check if future is resolved.
   *
   * @return {@code true} if the future is in a resolved state, otherwise {@code false}.
   */
  boolean isCompleted();

  /**
   * Check if the future was exceptionally completed.
   *
   * @return {@code true} if the stage is exceptionally completed
   * @see eu.toolchain.concurrent.CompletableFuture#fail(Throwable)
   */
  boolean isFailed();

  /**
   * Check if future is cancelled.
   *
   * @return {@code true} if the future is in a cancelled state, otherwise {@code false}.
   */
  boolean isCancelled();

  /**
   * Get the cause of a failed future.
   *
   * @return the exception that cause the future to fail
   * @throws IllegalStateException if the future is not in the failed state
   * @see #isFailed()
   */
  Throwable cause();

  /**
   * Register a future that will be cancelled by this future.
   *
   * @param other other future to bind to
   * @return this future
   */
  CompletionStage<T> bind(CompletionStage<?> other);

  /**
   * Register a listener that is called on all three types of events for this future; resolved,
   * failed, and cancelled.
   *
   * @param handle handle to register
   * @return this future
   */
  CompletionStage<T> handle(CompletionHandle<? super T> handle);

  /**
   * Register a listener to be called when this future finishes for any reason.
   *
   * @param runnable function to be fired
   * @return this future
   */
  CompletionStage<T> whenFinished(Runnable runnable);

  /**
   * Register an listener to be called when this future is cancelled.
   *
   * @param runnable listener to register
   * @return this future
   */
  CompletionStage<T> whenCancelled(Runnable runnable);

  /**
   * Register a listener to be called when this future is resolved.
   *
   * @param consumer listener to register
   * @return this future
   */
  CompletionStage<T> whenCompleted(Consumer<? super T> consumer);

  /**
   * Register a listener that is called when a future is failed.
   *
   * @param consumer listener to register
   * @return this future
   */
  CompletionStage<T> whenFailed(Consumer<? super Throwable> consumer);

  /**
   * Transforms the value of this future into another type using a transformer function.
   * <p>
   * <pre>
   * Future<T> (this) - *using transformer* -> Future<C>
   * </pre>
   * <p>
   * Use this if the transformation performed does not require any more async operations.
   * <p>
   * <pre>
   * {@code
   *   Future<Integer> first = asyncOperation();
   *   Future<Double> second = future.thenApply(result -> result.doubleValue());
   *
   *   # use second
   * }
   * </pre>
   *
   * @param <U> the type of the newly applied future
   * @param fn transformation to use
   * @return the applied future
   */
  <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

  /**
   * Transforms the value of one future into another using a deferred transformer function.
   * <p>
   * <pre>
   * Future<T> (this) - *using deferred transformer* -> Future<C>
   * </pre>
   * <p>
   * A deferred transformer is expected to return a compatible future that when resolved will
   * complete the future that this function returns.
   * <p>
   * <pre>
   * {@code
   *   Future<Integer> first = asyncOperation();
   *   Future<Double> second = first.thenCompose(result -> otherAsyncOperation(result));
   *
   *   # use second
   * }
   * </pre>
   *
   * @param <U> type of the composed future
   * @param fn the function to use when transforming the value
   * @return the composed future
   */
  <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

  /**
   * Apply a failed future.
   *
   * @param fn the transformation to use
   * @return the applied future
   */
  CompletionStage<T> thenCatchFailed(Function<? super Throwable, ? extends T> fn);

  /**
   * Compose a failed future.
   *
   * @param fn the transformation to use
   * @return the composed future
   */
  CompletionStage<T> thenComposeFailed(
      Function<? super Throwable, ? extends CompletionStage<T>> fn
  );

  /**
   * Transform something cancelled into something useful.
   *
   * @param supplier supplier to get value from
   * @return the applied future
   */
  CompletionStage<T> thenCatchCancelled(Supplier<? extends T> supplier);

  /**
   * Supply an a future when this future is cancelled
   *
   * @param supplier supplier to get future from
   * @return the composed future
   */
  CompletionStage<T> thenComposeCancelled(Supplier<? extends CompletionStage<T>> supplier);
}
