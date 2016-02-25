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
     * @throws Exception if the cancelled future cannot be handled, will <em>not</em> cause the
     * target future to fail. Behavior is defined by the implementation of {@link
     * AsyncCaller#cancel(FutureCancelled)}.
     * @see AsyncCaller#cancel(FutureCancelled)
     */
    public void cancelled() throws Exception;
}
