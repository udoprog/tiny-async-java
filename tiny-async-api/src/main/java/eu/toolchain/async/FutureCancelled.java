package eu.toolchain.async;

/**
 * Handle to be called when the underlying future is cancelled.
 *
 * @author udoprog
 */
public interface FutureCancelled {
    /**
     * Handle to be called when the underlying future is cancelled.
     *
     * @param value The resolved value.
     * @throws Exception if the cancelled future cannot be handled, will cause the target future to be failed.
     */
    public void cancelled() throws Exception;
}
