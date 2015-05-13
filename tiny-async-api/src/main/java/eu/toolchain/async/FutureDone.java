package eu.toolchain.async;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @author udoprog
 */
public interface FutureDone<T> {
    /**
     * Handle to be called when the underlying future is failed.
     *
     * @param cause The reason that the underlying future is failed.
     * @throws Exception if the failed future cannot be handled, will cause the target future to be failed.
     */
    void failed(Throwable cause) throws Exception;

    /**
     * Handle to be called when the underlying future is resolved.
     *
     * @param value The resolved value.
     * @throws Exception if the resolved future cannot be handled, will cause the target future to be failed.
     */
    void resolved(T result) throws Exception;

    /**
     * The future was cancelled.
     *
     * @throws Exception if unable to handle the cancelled future, will cause the target future to be failed.
     */
    void cancelled() throws Exception;
}