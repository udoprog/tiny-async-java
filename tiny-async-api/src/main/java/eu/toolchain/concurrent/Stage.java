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
 * <p>All methods are <em>thread-safe</em> guaranteeing that interactions with the stage are atomic.
 *
 * <p>A stage has four states:<ul>
 *
 * <li><em>pending</em>, which indicates that the stage is awaiting completion.</li>
 *
 * <li><em>completed</em>, which indicates that the stage has been completed.</li>
 *
 * <li><em>failed</em>, the stage has failed to be completed.</li>
 *
 * <li><em>cancelled</em>, the stage has been cancelled.</li></ul>
 *
 * <p>The last three states are characterized as <em>end states</em>, a completable can only
 * transition into one of these, and when in an end-state will never go into another state. If a
 * completable is in and end state it is considered <em>done</em>, as is indicated by the
 * {@link #isDone()} method.
 *
 * @param <T> the type being provided by the completable
 * @author udoprog
 * @see Completable
 */
public interface Stage<T> {
  /**
   * Cancel the current stage.
   *
   * @return {@code true} if the stage was cancelled by this call
   */
  boolean cancel();

  /**
   * Join the result of the current stage.
   *
   * @return the result of the computation
   * @throws ExecutionException when the underlying computation throws an exception
   * @throws CancellationException when the computation was cancelled
   * @throws InterruptedException when this thread is interrupted
   */
  T join() throws ExecutionException, InterruptedException;

  /**
   * Join the result of the current stage with a timeout.
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
   * Join the result of the current stage, or fail if it's state is still running.
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
   * @return {@code true} if the completable is completed, failed, or cancelled
   */
  boolean isDone();

  /**
   * Check if completable is completed.
   *
   * @return {@code true} if the completable is in a completed state, otherwise {@code false}.
   */
  boolean isCompleted();

  /**
   * Check if the completable was exceptionally completed.
   *
   * @return {@code true} if the stage is exceptionally completed
   * @see Completable#fail(Throwable)
   */
  boolean isFailed();

  /**
   * Check if completable is in the cancelled state.
   *
   * @return {@code true} if the completable is in a cancelled state, otherwise {@code false}.
   */
  boolean isCancelled();

  /**
   * Get the cause of a failed stage.
   *
   * @return the exception that cause the completable to fail
   * @throws IllegalStateException if the completable is not in the failed state
   * @see #isFailed()
   */
  Throwable cause();

  /**
   * Register a listener that is called on all three types of events for this completable; completed,
   * failed, and cancelled.
   *
   * @param handle handle to register
   * @return this completable
   */
  Stage<T> whenDone(CompletionHandle<? super T> handle);

  /**
   * Register a listener to be called when this completable finishes for any reason.
   *
   * @param runnable function to be fired
   * @return this completable
   */
  Stage<T> whenFinished(Runnable runnable);

  /**
   * Register a listener to be called when this completable is completed.
   *
   * @param consumer listener to register
   * @return this completable
   */
  Stage<T> whenComplete(Consumer<? super T> consumer);

  /**
   * Register a listener that is called when a completable is failed.
   *
   * @param consumer listener to register
   * @return this completable
   */
  Stage<T> whenFailed(Consumer<? super Throwable> consumer);

  /**
   * Register an listener to be called when this completable is cancelled.
   *
   * @param runnable listener to register
   * @return this completable
   */
  Stage<T> whenCancelled(Runnable runnable);

  /**
   * Transform the value of this completable into another type using an immediate function.
   *
   * <p>Translates the result of a completed completable as it becomes available:
   *
   * <pre>{@code
   *   operation().thenApply(result -> result / 2);
   * }</pre>
   *
   * @param <U> the type of the newly applied completable
   * @param fn transformation to use
   * @return the applied completable
   */
  <U> Stage<U> thenApply(Function<? super T, ? extends U> fn);

  /**
   * Compose the current stage with the given function.
   *
   * <p>When the current stage has completed, calls the given function with the result of the
   * current stage.
   *
   * <pre>{@code
   *   Stage<A> a = op1();
   *   Stage<B> a.thenCompose(result -> op2(result));
   * }</pre>
   *
   * @param <U> type of the composed completable
   * @param fn the function to use when transforming the value
   * @return the composed completable
   */
  <U> Stage<U> thenCompose(Function<? super T, ? extends Stage<U>> fn);

  /**
   * Apply a failed completable.
   *
   * @param fn the transformation to use
   * @return the applied completable
   */
  Stage<T> thenApplyFailed(Function<? super Throwable, ? extends T> fn);

  /**
   * Compose a failed completable.
   *
   * @param fn the transformation to use
   * @return the composed completable
   */
  Stage<T> thenComposeFailed(
      Function<? super Throwable, ? extends Stage<T>> fn
  );

  /**
   * Transform something cancelled into something useful.
   *
   * @param supplier supplier to get value from
   * @return the applied completable
   */
  Stage<T> thenApplyCancelled(Supplier<? extends T> supplier);

  /**
   * Supply an a completable when this completable is cancelled
   *
   * @param supplier supplier to get completable from
   * @return the composed completable
   */
  Stage<T> thenComposeCancelled(Supplier<? extends Stage<T>> supplier);
}
