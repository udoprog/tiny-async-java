package eu.toolchain.concurrent;

/**
 * Managed lightweight, reference-counted objects that can be reloaded.
 *
 * @param <T> The type of the object being managed.
 * @author udoprog
 */
public interface ReloadableManaged<T> extends Managed<T> {
  /**
   * Reload the underlying reference.
   * <p>
   * The new reference will be constructed before the old one is shut down.
   *
   * @param startFirst Start the new managed reference before shutting down the old one.
   * @return A future that will be resolved once the reference has been reloaded.
   */
  CompletionStage<Void> reload(boolean startFirst);
}
