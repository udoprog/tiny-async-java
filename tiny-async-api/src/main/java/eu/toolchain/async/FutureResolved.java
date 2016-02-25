package eu.toolchain.async;

/**
 * Handle to implement to be called when the future is resolved.
 *
 * @param <T> the type of the underlying future.
 * @author udoprog
 */
public interface FutureResolved<T> {
    /**
     * Handle to be called when the underlying future is resolved.
     *
     * @param result The resolved value.
     * @throws Exception if the resolved future cannot be handled, will not cause the target future
     * to be failed. Behavior is defined by the implementation of {@link
     * AsyncCaller#resolve(FutureResolved, Object)}.
     * @see AsyncCaller#resolve(FutureResolved, Object)
     */
    public void resolved(T result) throws Exception;
}
