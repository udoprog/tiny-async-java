package eu.toolchain.concurrent;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @param <T> type of the future to listen on.
 * @author udoprog
 */
public interface ApplyHandle<T, U> {
  /**
   * Handle to be called when the underlying future is completed and apply to a new value.
   *
   * @param result The result of the completed future.
   */
  U completed(T result);

  /**
   * Handle to be called when the underlying future is failed and apply to a new value.
   *
   * @param cause Exception that caused the underlying future to except.
   */
  U failed(Throwable cause);

  /**
   * Handle to be called when the underlying future is cancelled and apply to a new value.
   */
  U cancelled();
}
