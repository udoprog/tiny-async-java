package eu.toolchain.async;

/**
 * A future that can be resolved, or failed programmatically.
 * <p>
 * This is typically the 'other end' of an asynchronous computation. By separating the contract we
 * limit the capabilities that {@code AsyncFuture} has to implement, which allows for
 * optimizations.
 *
 * @param <T> The type being provided by the future.
 * @author udoprog
 * @see AsyncFuture
 */
public interface ResolvableFuture<T> extends AsyncFuture<T> {
    /**
     * Resolve the future.
     * <p>
     * This method could cause the calling thread to execute result listeners.
     *
     * @param result Result to provide to the future.
     * @return {@code true} if the future was resolved.
     */
    public boolean resolve(T result);

    /**
     * Fail the future.
     *
     * @param cause What caused the future to be failed.
     * @return {@code true} if the future was failed because of this call.
     */
    @Override
    public boolean fail(Throwable cause);
}
