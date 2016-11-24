package eu.toolchain.concurrent;

/**
 * Handle to implement that can catch all the different states of a stage.
 *
 * @param <T> type of the stage to listen on
 */
public interface Handle<T> {
  /**
   * Handle to be called when the underlying stage is completed.
   *
   * @param result The result of the completed stage.
   */
  void completed(T result);

  /**
   * Handle to be called when the underlying stage is failed.
   *
   * @param cause exception that caused the underlying stage to fail
   */
  void failed(Throwable cause);

  /**
   * Handle to be called when the underlying stage is cancelled.
   */
  void cancelled();
}
