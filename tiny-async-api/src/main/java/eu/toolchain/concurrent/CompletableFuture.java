package eu.toolchain.concurrent;

/**
 * A future that can be completed.
 *
 * <p>This is typically the 'other end' of an asynchronous computation. By separating the contract we
 * limit the capabilities that {@code CompletionStage} has to implement, which allows for
 * optimizations.
 *
 * @param <T> The type being provided by the future.
 * @author udoprog
 * @see CompletionStage
 */
public interface CompletableFuture<T> extends CompletionStage<T> {
  /**
   * If not already completed, completes the future with the given value.
   * <p>
   * This method might cause the calling thread to execute listeners.
   *
   * @param result result of the computation
   * @return {@code true} if the future was completed by this call
   */
  boolean complete(T result);

  /**
   * If not already completed, completes the future exceptionally.
   *
   * @param cause the cause of the failure
   * @return {@code true} if the future was completed by this call
   */
  boolean fail(Throwable cause);
}
