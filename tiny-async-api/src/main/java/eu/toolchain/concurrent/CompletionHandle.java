package eu.toolchain.concurrent;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @param <T> type of the future to listen on.
 * @author udoprog
 */
public interface CompletionHandle<T> {
  /**
   * Handle to be called when the underlying future is failed.
   *
   * @param cause Exception that caused the underlying future to except.
   * @throws Exception if the failed future cannot be handled, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   * FutureCaller#fail(CompletionHandle, Throwable)}.
   * @see FutureCaller#fail(CompletionHandle, Throwable)
   */
  void failed(Throwable cause) throws Exception;

  /**
   * Handle to be called when the underlying future is resolved.
   *
   * @param result The result of the resolved future.
   * @throws Exception if the resolved future cannot be handled, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   * FutureCaller#resolve(CompletionHandle, Object)}.
   * @see FutureCaller#resolve(CompletionHandle, Object)
   */
  void resolved(T result) throws Exception;

  /**
   * Handle to be called when the underlying future is cancelled.
   *
   * @throws Exception if unable to handle the cancelled future, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   * FutureCaller#cancel(CompletionHandle)}.
   * @see FutureCaller#cancel(CompletionHandle)
   */
  void cancelled() throws Exception;
}
