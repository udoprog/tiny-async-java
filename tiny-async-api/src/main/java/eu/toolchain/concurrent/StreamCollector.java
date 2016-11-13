package eu.toolchain.concurrent;

/**
 * Collect the result of multiple asynchronous computations as they become available.
 *
 * @param <T> The source type of the collector.
 * @param <U> The target type of the collector.
 * @author udoprog
 */
public interface StreamCollector<T, U> {
  /**
   * Is called when a future is completed.
   *
   * @param result The result of the completed future.
   * @throws Exception if unable to process the result of the future, this will cause the target
   * future to be failed. {@link #end(int, int, int)} will not be called, and all other futures
   * associated with the collector will be cancelled.
   */
  void completed(T result);

  /**
   * Is called when a future is failed.
   *
   * @param cause The cause of the failed future.
   * @throws Exception if unable to process the failed future, this will cause the target future to
   * be failed. {@link #end(int, int, int)} will not be called will not be called, and all other
   * futures associated with the collector will be cancelled.
   */
  void failed(Throwable cause);

  /**
   * Is called when a future is cancelled.
   *
   * @throws Exception if unable to process the cancelled future, this will cause the target future
   * to be failed. {@link #end(int, int, int)} will not be called, and all other futures associated
   * with the collector will be cancelled.
   */
  void cancelled();

  /**
   * Implement to fire when all callbacks have been completed.
   *
   * @param resolved How many of the collected futures were completed.
   * @param failed How many of the collected futures were failed.
   * @param cancelled How many of the collected futures were cancelled.
   * @return The collected value.
   * @throws Exception if unable to process the results of the colllection, this will cause the
   * target future to be failed. {@link #end(int, int, int)} will not be called will not be called,
   * and all other futures associated with the collector will be cancelled.
   */
  U end(int resolved, int failed, int cancelled);
}
