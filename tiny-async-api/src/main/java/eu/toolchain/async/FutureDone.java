package eu.toolchain.async;

/**
 * Handle to implement when catching a future's transition into done.
 *
 * Implement methods to figure out what happened.
 */
public interface FutureDone<T> {
    /**
     * The done future failed.
     *
     * @param cause The cause of the failure.
     * @throws Exception If the handle throws an exception.
     */
    void failed(Throwable cause) throws Exception;

    /**
     * The done future was resolved.
     *
     * @param result The result from the resolved future.
     * @throws Exception If the handle throws an exception.
     */
    void resolved(T result) throws Exception;

    /**
     * The future was cancelled.
     *
     * @throws Exception If the handle throws an exception.
     */
    void cancelled() throws Exception;
}