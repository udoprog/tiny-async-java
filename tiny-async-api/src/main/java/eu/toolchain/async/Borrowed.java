package eu.toolchain.async;

/**
 * A reference to an object that has been <em>borrowed</em>, which can prevent certain actions from being executed as
 * long as the borrowed reference is valid.
 *
 * <p>
 * {@code null} is not a valid borrowed reference, any attempt to borrow a null reference is explicitly prohibited by
 * the framework.
 * </p>
 *
 * <h4>Reference Counting</h4>
 *
 * <p>
 * Borrowed references are <em>reference counted</em>, the user is responsible for releasing the reference when it is no
 * longer used. Use of convenience methods like {@code #releasing()} are encouraged to properly accomplish this.
 * </p>
 *
 * @author udoprog
 *
 * @param <T> The type of the borrowed reference.
 */
public interface Borrowed<T> extends AutoCloseable {
    /**
     * Return {@code true} if this borrowed reference is valid.
     *
     * A valid borrowed reference is guaranteed to have a value.
     *
     * @return
     */
    public boolean isValid();

    /**
     * Fetch the borrowed reference.
     *
     * @throws IllegalStateException if the borrowed reference is not valid.
     * @return The borrowed reference.
     */
    public T get();

    /**
     * Release the borrowed reference.
     *
     * A borrowed reference can only be released once. This is required to allow the underlying framework to free up the
     * borrowed reference when it is no longer used.
     */
    public void release();

    /**
     * Convenience method for binding the release of a borrowed reference to a future.
     *
     * <p>
     * Below is a typical usage of {@link #releasing()}.
     * </p>
     *
     * <pre>
     * {@code
     * final Managed<Object> managed = ...;
     *
     * final Borrowed<Object> b = managed.borrow();
     *
     * if (!b.isValid())
     *   return async.cancelled();
     *
     * final AsyncFuture<Object> future = doSomethingAsync(b.get());
     * return future.on(b.releasing());
     * }
     * </pre>
     *
     * @return
     */
    public FutureFinished releasing();

    /**
     * The close method, as defined by {@link AutoCloseable#close()} to allow for try-with-resources statements.
     *
     * Override of {@link AutoCloseable#close()} to remove throws signature.
     */
    @Override
    public void close();
}