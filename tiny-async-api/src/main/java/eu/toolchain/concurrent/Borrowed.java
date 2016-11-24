package eu.toolchain.concurrent;

/**
 * A borrowed reference.
 *
 * <p>Borrowed references can prevent certain actions from being executed as long as the borrowed
 * reference is valid.
 *
 * <p>{@code null} is not a valid borrowed reference, any attempt to borrow a null reference is
 * explicitly prohibited by the framework.
 *
 * <p>Borrowed references are <em>reference counted</em>, the user is responsible for releasing the
 * reference when it is no longer used. Use of convenience methods like {@link
 * Managed#doto(java.util.function.Function)} are encouraged to accomplish this.
 *
 * @param <T> type of the borrowed reference
 */
public interface Borrowed<T> extends AutoCloseable {
  /**
   * Check if the borrowed reference is valid.
   * <p>
   * A valid borrowed reference is guaranteed to have a value.
   *
   * @return {@code true} if the borrowed reference is valid.
   */
  boolean isValid();

  /**
   * Fetch the borrowed reference.
   *
   * @return the borrowed reference
   * @throws IllegalStateException if the borrowed reference is not valid
   */
  T get();

  /**
   * Release the borrowed reference.
   *
   * <p>A borrowed reference can only be released once. This is required to allow the underlying
   * framework to free up the borrowed reference when it is no longer used.
   */
  void release();

  /**
   * The close method, as defined by {@link AutoCloseable#close()} to allow for try-with-resources
   * statements.
   *
   * <p>Override of {@link AutoCloseable#close()} to remove throws signature.
   */
  @Override
  void close();
}
