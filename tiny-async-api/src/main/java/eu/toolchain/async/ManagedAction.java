package eu.toolchain.async;

/**
 * A managed action.
 */
public interface ManagedAction<T, R> {
    public AsyncFuture<R> action(T value) throws Exception;
}