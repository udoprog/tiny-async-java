package eu.toolchain.async;

/**
 * Handle to implement that can catch all the different future states.
 *
 * @author udoprog
 * @param <T> type of the future to listen on.
 */
public interface FutureDone<T> {
    /**
     * Handle to be called when the underlying future is failed.
     *
     * @param cause Exception that caused the underlying future to fail.
     * @throws Exception if the failed future cannot be handled, will <em>not</em> cause the target future to be failed.
     *             Behavior is defined by the implementation of {@link AsyncCaller#fail(FutureDone, Throwable)}.
     * @see AsyncCaller#fail(FutureDone, Throwable)
     */
    void failed(Throwable cause) throws Exception;

    /**
     * Handle to be called when the underlying future is resolved.
     *
     * @param result The result of the resolved future.
     * @throws Exception if the resolved future cannot be handled, will <em>not</em> cause the target future to be
     *             failed. Behavior is defined by the implementation of {@link AsyncCaller#resolve(FutureDone, Object)}.
     * @see AsyncCaller#resolve(FutureDone, Object)
     */
    void resolved(T result) throws Exception;

    /**
     * Handle to be called when the underlying future is cancelled.
     *
     * @throws Exception if unable to handle the cancelled future, will <em>not</em> cause the target future to be
     *             failed. Behavior is defined by the implementation of {@link AsyncCaller#cancel(FutureDone)}.
     * @see AsyncCaller#cancel(FutureDone)
     */
    void cancelled() throws Exception;
}