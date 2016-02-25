package eu.toolchain.async;

/**
 * Sets up a constructor, and destructor of a managed reference.
 *
 * @param <T> The type of the managed reference.
 * @author udoprog
 */
public interface ManagedSetup<T> {
    /**
     * Construct a managed reference.
     *
     * @return A future that will construct the managed reference.
     * @throws Exception if the managed reference cannot be constructed.
     */
    public AsyncFuture<T> construct() throws Exception;

    /**
     * Destruct the managed reference.
     *
     * @param value The managed reference to destruct.
     * @return A future that when resolved indicates that the managed reference was destructed.
     * @throws Exception if the managed reference cannot be destructed.
     */
    public AsyncFuture<Void> destruct(T value) throws Exception;
}
