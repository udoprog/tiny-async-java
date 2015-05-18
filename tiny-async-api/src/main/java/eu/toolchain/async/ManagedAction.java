package eu.toolchain.async;

/**
 * An action that acts on a managed reference.
 *
 * <p>
 * Managed references can be borrowed, but that can cause issues since the user has to explicitly remember to free the
 * reference after a computation. {@code ManagedAction} however, asserts that the reference being acted on is valid, and
 * remains borrowed either until an exception has been thrown, or the returned future is finished.
 * </p>
 *
 * @author udoprog
 * @param <T> the type of the reference being borrowed.
 * @param <R> the type of the returned future.
 */
public interface ManagedAction<T, R> {
    /**
     * The action to execute against the managed reference.
     *
     * @param reference The reference that has been borrowed.
     * @return A future, that when finished will release the borrowed reference.
     * @throws Exception If the action cannot be executed, this will release the borrowed reference.
     */
    public AsyncFuture<R> action(T reference) throws Exception;
}