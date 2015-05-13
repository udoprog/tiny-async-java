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
     * @throws Exception if the failed future cannot be handled, will cause the target future to be failed.
     */
    public void failed(Throwable cause) throws Exception;
}