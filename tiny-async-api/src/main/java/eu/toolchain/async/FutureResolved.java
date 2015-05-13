package eu.toolchain.async;

/**
 * Handle to implement to be called when the future is resolved.
 *
 * @author udoprog
 */
public interface FutureResolved<T> {
    /**
     * Handle to be called when the underlying future is resolved.
     *
     * @param value The resolved value.
     * @throws Exception if the resolved future cannot be handled.
     */
    public void resolved(T value) throws Exception;
}