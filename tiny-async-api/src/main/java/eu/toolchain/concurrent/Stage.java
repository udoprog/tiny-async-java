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
 * <li><em>cancelled</em>, the stage has been cancelled.</li>
 *
 * </ul>
 *
 * <p>The last three states are characterized as <em>end states</em>, a stage can only
 * transition into one of these, and when in an end-state will never go into another state. If a
 * stage is in and end state it is considered <em>done</em>, as is indicated by the
 * {@link #isDone()} method.
 *
 * @param <T> the type being provided by the stage
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
   * @return {@code true} if the stage is completed, failed, or cancelled
   */
  boolean isDone();

  /**
   * Check if stage is completed.
   *
   * @return {@code true} if the stage is in a completed state, otherwise {@code false}.
   */
  boolean isCompleted();

  /**
   * Check if the stage was exceptionally completed.
   *
   * @return {@code true} if the stage is exceptionally completed
   * @see Completable#fail(Throwable)
   */
  boolean isFailed();

  /**
   * Check if stage is in the cancelled state.
   *
   * @return {@code true} if the stage is in a cancelled state, otherwise {@code false}.
   */
  boolean isCancelled();

  /**
   * Get the cause of a failed stage.
   *
   * @return the exception that cause the stage to fail
   * @throws IllegalStateException if the stage is not in the failed state
   * @see #isFailed()
   */
  Throwable cause();

  /**
   * Register a listener that is called on all three types of events for the current stage; completed,
   * failed, and cancelled.
   *
   * @param handle handle to register
   * @return the current stage
   */
  Stage<T> handle(Handle<? super T> handle);

  /**
   * Register a listener to be called when the current stage finishes for any reason.
   *
   * @param runnable function to be fired
   * @return the current stage
   */
  Stage<T> whenDone(Runnable runnable);

  /**
   * Register a listener to be called when the current stage is completed.
   *
   * @param consumer listener to register
   * @return the current stage
   */
  Stage<T> whenComplete(Consumer<? super T> consumer);

  /**
   * Register a listener that is called when a stage is failed.
   *
   * @param consumer listener to register
   * @return the current stage
   */
  Stage<T> whenFailed(Consumer<? super Throwable> consumer);

  /**
   * Register an listener to be called when the current stage is cancelled.
   *
   * @param runnable listener to register
   * @return the current stage
   */
  Stage<T> whenCancelled(Runnable runnable);

  /**
   * Transform the value of the current stage into another type using an immediate function.
   *
   * <p>Translates the result of a completed stage as it becomes available:
   *
   * <pre>{@code
   *   operation().thenApply(result -> result / 2);
   * }</pre>
   *
   * @param <U> the type of the newly applied stage
   * @param fn transformation to use
   * @return the applied stage
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
   * @param <U> type of the composed stage
   * @param fn the function to use when transforming the value
   * @return the composed stage
   */
  <U> Stage<U> thenCompose(Function<? super T, ? extends Stage<U>> fn);

  /**
   * Apply a failed stage.
   *
   * <p>This acts like a <em>try-catch</em>, where the provided function is the catch operation.
   *
   * @param fn the transformation to use
   * @return the applied stage
   */
  Stage<T> thenApplyCaught(Function<? super Throwable, ? extends T> fn);

  /**
   * Compose a failed stage.
   *
   * <p>This acts like a <em>try-catch</em>, where the composed stage is the catch operation.
   *
   * <p>If the intent is to re-throw after running the catch operation, use
   * {@link #withNotComplete(java.util.function.Supplier)}
   *
   * @param fn the transformation to use
   * @return the composed stage
   */
  Stage<T> thenComposeCaught(Function<? super Throwable, ? extends Stage<T>> fn);

  /**
   * Run one of the provided stages when this stage ends up in a given state.
   *
   * <p>This is typically used to implement catch-then-rethrow style operations, like the following
   * example:
   *
   * <pre>{@code
   *   beginTransaction().thenCompose(tx -> {
   *     return oper2(tx).withCloser(tx::commit, tx::rollback);
   *   });
   * }</pre>
   *
   * @param complete stage to run when this stage ends in a complete state
   * @param notComplete stage to run when this stage ends in a non-complete state, if the state is
   * failed, any exception thrown in this block will suppress the original exception
   * @return a new stage that depends on the supplied stage
   */
  Stage<T> withCloser(
      Supplier<? extends Stage<Void>> complete, Supplier<? extends Stage<Void>> notComplete
  );

  /**
   * Run the provided stage when the current stage has completed.
   *
   * @param supplier supplier of the stage to run
   * @return a stage that depends on the current stage and the supplied stage
   */
  Stage<T> withComplete(Supplier<? extends Stage<Void>> supplier);

  /**
   * Run the provided stage when the current stage ends, but does not complete.
   *
   * @param supplier supplier of the stage to run
   * @return a stage that depends on the current stage and the supplied stage
   */
  Stage<T> withNotComplete(Supplier<? extends Stage<Void>> supplier);

  /**
   * Build a stage is failed, but waits until the current stage completes.
   *
   * @param cause error to fail with
   * @param <U> target type
   * @return a stage that will be failed
   */
  <U> Stage<U> thenFail(Throwable cause);

  /**
   * Build a stage is cancelled, but waits until the current stage completes.
   *
   * @param <U> target type
   * @return a stage that will be cancelled
   */
  <U> Stage<U> thenCancel();

  /**
   * Build a stage is completed, but waits until the current stage completes.
   *
   * @param result result to complete with
   * @param <U> target type
   * @return a stage that will be completed
   */
  <U> Stage<U> thenComplete(U result);
}
