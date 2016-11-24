package eu.toolchain.concurrent;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @param <T> type of the future to listen on
 */
public interface Handle<T> {
  /**
   * Handle to be called when the underlying future is completed.
   *
   * @param result The result of the completed future.
   */
  void completed(T result);

  /**
   * Handle to be called when the underlying future is failed.
   *
   * @param cause exception that caused the underlying stage to fail
   */
  void failed(Throwable cause);

  /**
   * Handle to be called when the underlying future is cancelled.
   */
  void cancelled();
}
