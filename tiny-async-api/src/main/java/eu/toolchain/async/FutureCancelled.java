package eu.toolchain.async;

public interface FutureCancelled {
    /**
     * Called when future is cancelled.
     */
    public void cancelled() throws Exception;
}
