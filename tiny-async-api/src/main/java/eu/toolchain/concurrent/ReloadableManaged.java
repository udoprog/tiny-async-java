package eu.toolchain.concurrent;

/**
 * Managed lightweight, reference-counted objects that can be reloaded.
 *
 * @param <T> type of the object being managed
 */
public interface ReloadableManaged<T> extends Managed<T> {
  /**
   * Reload the underlying reference.
   *
   * <p>The new reference will be constructed and started before the old one is shut down.
   *
   * @return a stage that will be completed once the reference has been reloaded
   */
  Stage<Void> reload();
}
