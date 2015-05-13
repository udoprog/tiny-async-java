package eu.toolchain.async;

/**
 * Collect the result of multiple asynchronous computations as they become available.
 *
 * @author udoprog
 *
 * @param <S> The source type of the collector.
 * @param <T> The target type of the collector.
 */
public interface StreamCollector<S, T> {
    /**
     * Is called when a future is resolved.
     *
     * @param result The result of the resolved future.
     * @throws Exception if unable to process the result of the future, this will cause the target future to be failed.
     *             {@link #end(int, int, int)} will not be called, and all other futures associated with the collector
     *             will be cancelled.
     */
    void resolved(S result) throws Exception;

    /**
     * Is called when a future is failed.
     *
     * @param cause The cause of the failed future.
     * @throws Exception if unable to process the failed future, this will cause the target future to be failed.
     *             {@link #end(int, int, int)} will not be called will not be called, and all other futures associated
     *             with the collector will be cancelled.
     */
    void failed(Throwable cause) throws Exception;

    /**
     * Is called when a future is cancelled.
     *
     * @throws Exception if unable to process the cancelled future, this will cause the target future to be failed.
     *             {@link #end(int, int, int)} will not be called, and all other futures associated with the collector
     *             will be cancelled.
     */
    void cancelled() throws Exception;

    /**
     * Implement to fire when all callbacks have been resolved.
     *
     * @param resolved How many of the collected futures were resolved.
     * @param failed How many of the collected futures were failed.
     * @param cancelled How many of the collected futures were cancelled.
     */
    T end(int resolved, int failed, int cancelled) throws Exception;
}