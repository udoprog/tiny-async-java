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
 *
 * <p>All methods exposed in {@code CompletionStage} are fully <em>thread-safe</em>, guaranteeing
 * that interactions with the future are atomic.
 *
 * <p>A future has four states:<ul>
 *
 * <li><em>running</em>, which indicates that the future is currently active, and has not reached an
 * end-state.</li>
 *
 * <li><em>completed</em>, which indicates that the computation was successful, and produced a
 * result.</li>
 *
 * <li><em>failed</em>, which indicates that the computation failed through an exception, which can
 * be fetched for inspection.</li>
 *
 * <li><em>cancelled</em>, which indicates that the computation was cancelled.</li></ul>
 *
 * <p>The last three states are characterized as <em>end states</em>, a future can only transition
 * into one of these, and when in an end-state will never go into another state. If a future is in
 * and end state it is considered <em>done</em>, as is indicated by the {@link #isDone()} method.
 *
 * <p>A stage is typically used in combination with future computations.
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
   * @throws ExecutionException when the underlying computation throws an exception
   * @throws CancellationException when the computation was cancelled
   * @throws InterruptedException when this thread is interrupted
   */
  T join() throws ExecutionException, InterruptedException;

  /**
   * Get the result of the future.
   *
   * @param timeout timeout after which {@link java.util.concurrent.TimeoutException} will be
   * thrown.
   * @param unit unit of the timeout
   * @return the result of the computation
   * @throws ExecutionException when the underlying computation throws an exception
   * @throws CancellationException if the computation was cancelled
   * @throws TimeoutException when the computation does not finish within the given timeout
   * @throws InterruptedException when this thread is interrupted
   */
  T join(long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException;

  /**
   * Get the result of the future without blocking.
   *
   * @return the result of the computation
   * @throws IllegalStateException if the result is not available
   * @throws ExecutionException when the underlying computation throws an exception
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
   * Check if future is completed.
   *
   * @return {@code true} if the future is in a completed state, otherwise {@code false}.
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
   * Register a listener that is called on all three types of events for this future; completed,
   * failed, and cancelled.
   *
   * @param handle thenHandle to register
   * @return this future
   */
  CompletionStage<T> thenHandle(CompletionHandle<? super T> handle);

  /**
   * Register a listener to be called when this future finishes for any reason.
   *
   * @param runnable function to be fired
   * @return this future
   */
  CompletionStage<T> whenFinished(Runnable runnable);

  /**
   * Register a listener to be called when this future is completed.
   *
   * @param consumer listener to register
   * @return this future
   */
  CompletionStage<T> whenComplete(Consumer<? super T> consumer);

  /**
   * Register a listener that is called when a future is failed.
   *
   * @param consumer listener to register
   * @return this future
   */
  CompletionStage<T> whenFailed(Consumer<? super Throwable> consumer);

  /**
   * Register an listener to be called when this future is cancelled.
   *
   * @param runnable listener to register
   * @return this future
   */
  CompletionStage<T> whenCancelled(Runnable runnable);

  /**
   * Transform the value of this future into another type using an immediate function.
   *
   * <p>Translates the result of a completed future as it becomes available:
   *
   * <pre>{@code
   *   operation().thenApply(result -> result / 2);
   * }</pre>
   *
   * @param <U> the type of the newly applied future
   * @param fn transformation to use
   * @return the applied future
   */
  <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

  /**
   * Compose the current stage with the given function.
   *
   * <p>When the current stage has completed, calls the given function with the result of the
   * current stage.
   *
   * <pre>{@code
   *   CompletionStage<A> a = op1();
   *   CompletionStage<B> a.thenCompose(result -> op2(result));
   * }</pre>
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
  CompletionStage<T> thenApplyFailed(Function<? super Throwable, ? extends T> fn);

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
  CompletionStage<T> thenApplyCancelled(Supplier<? extends T> supplier);

  /**
   * Supply an a future when this future is cancelled
   *
   * @param supplier supplier to get future from
   * @return the composed future
   */
  CompletionStage<T> thenComposeCancelled(Supplier<? extends CompletionStage<T>> supplier);
}
