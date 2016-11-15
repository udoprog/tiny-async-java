package eu.toolchain.concurrent;

/**
 * Collect the result of multiple asynchronous computations as they become available.
 *
 * <p>Any handlers throwing an exception will cause the target future to be failed.
 * {@link #end(int, int, int)} will not be called will not be called, and all other futures
 * associated with the collector will be cancelled.
 *
 * @param <T> the source type
 * @param <U> the target type
 * @author udoprog
 */
public interface StreamCollector<T, U> {
  /**
   * Is called when a future is completed.
   *
   * @param result the result of the completed stage
   */
  void completed(T result);

  /**
   * Is called when a future is failed.
   *
   * @param cause the cause of the failed stage
   */
  void failed(Throwable cause);

  /**
   * Is called when a future is cancelled.
   */
  void cancelled();

  /**
   * Implement to fire when all callbacks have been completed.
   *
   * @param resolved How many of the collected futures were completed.
   * @param failed How many of the collected futures were failed.
   * @param cancelled How many of the collected futures were cancelled.
   * @return the result of the computation done by this collector
   */
  U end(int resolved, int failed, int cancelled);
}
