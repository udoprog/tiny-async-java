package eu.toolchain.concurrent;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @param <T> type of the future to listen on.
 * @author udoprog
 */
public interface Handle<T> {
  /**
   * Handle to be called when the underlying future is completed.
   *
   * @param result The result of the completed future.
   * @throws Exception if the completed future cannot be handled, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   */
  void completed(T result);

  /**
   * Handle to be called when the underlying future is failed.
   *
   * @param cause Exception that caused the underlying future to except.
   * @throws Exception if the failed future cannot be handled, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   */
  void failed(Throwable cause);

  /**
   * Handle to be called when the underlying future is cancelled.
   *
   * @throws Exception if unable to handle the cancelled future, will <em>not</em> cause the target
   * future to be failed. Behavior is defined by the implementation of {@link
   */
  void cancelled();
}
