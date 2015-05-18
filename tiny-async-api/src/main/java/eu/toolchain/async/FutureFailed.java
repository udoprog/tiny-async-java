package eu.toolchain.async;

/**
 * Handle to be called when the underlying future is failed.
 *
 * @author udoprog
 */
public interface FutureFailed {
    /**
     * Handle to be called when the underlying future is failed.
     *
     * @param cause The reason that the underlying future is failed.
     * @throws Exception if the failed future cannot be handled, will <em>not</em> cause the target future to be failed.
     *             Behavior is defined by the implementation of {@link AsyncCaller#fail(FutureFailed, Throwable)}.
     * @see AsyncCaller#fail(FutureFailed, Throwable)
     */
    public void failed(Throwable cause) throws Exception;
}