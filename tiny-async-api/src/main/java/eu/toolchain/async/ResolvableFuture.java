package eu.toolchain.async;

public interface ResolvableFuture<T> extends AsyncFuture<T>, FutureDone<T> {
    /**
     * Resolve the future.
     *
     * This method can be safely called from any thread,
     *
     * This method could cause the calling thread to execute result listeners.
     *
     * @param result Result of the callback.
     * @return {@code true} if the future was resolved. {@code false} otherwise.
     */
    public boolean resolve(T value);
}
