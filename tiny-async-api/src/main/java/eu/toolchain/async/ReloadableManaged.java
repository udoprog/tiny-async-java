package eu.toolchain.async;

/**
 * Managed lightweight, reference-counted objects that can be reloaded.
 *
 * @author udoprog
 *
 * @param <T> The type of the object being managed.
 */
public interface ReloadableManaged<T> extends Managed<T> {
    /**
     * Reload the underlying reference.
     *
     * The new reference will be constructed before the old one is shut down.
     *
     * @param startFirst Start the new managed reference before shutting down the old one.
     * @return A future that will be resolved once the reference has been reloaded.
     */
    public AsyncFuture<Void> reload(boolean startFirst);
}