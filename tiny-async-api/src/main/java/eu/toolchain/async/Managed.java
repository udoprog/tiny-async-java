package eu.toolchain.async;

/**
 * Managed lightweight, reference-counted objects.
 *
 * <p>
 * This utility class guarantees that the underlying reference has been initialized and that the block of code using it
 * will never operate on an invalid reference.
 * </p>
 *
 * <p>
 * An invalid reference, is an object that has been 'destructed', an example would be a database connection that has
 * been closed, any subsequent actions on it is very likely to cause an exception to be thrown. By wrapping the
 * connection in a managed reference, any block of code using the reference prevents it from being de-allocated until
 * that block of code has exited.
 * </p>
 *
 * <h1>Usage</h1>
 *
 * <p>
 * Since we cannot rely on the garbage collector being invoked in a timely fashion, the borrowed references has to be
 * manually released. Fortunately, there are some helpers to ease this like {@code #doto(AsyncFramework, ManagedAction)}
 * , the try-with-resources pattern on a {@code Borrowed} reference, and the {@code Borrowed#release()} method.
 * </p>
 *
 * <h2>Example using doto</h2>
 *
 * <p>
 * This pattern is useful if you have a block of code returning a future.
 * </p>
 *
 * <p>
 * It guarantees that the code is executed in a safe fashion, that will release the reference if it throws an exception.
 * After it has successfully returned a future, the borrowed reference will be relased when this future is resolved.
 * </p>
 *
 * <pre>
 * {@code
 *   final Managed<T> m = ...;
 * 
 *   final AsyncFuture<Integer> future = m.doto(new ManagedAction<T, Integer>() {
 *     AsyncFuture<Integer> action(final T value) {
 *       // do something with value
 * 
 *       return async.resolved(42);
 *     }
 *   });
 * 
 *   try (final Borrowed<T> b = m.borrow()) {
 *     final T value = b.get();
 * 
 *     // do something with 'value'.
 *   }
 * }
 * </pre>
 *
 * <h2>Example using try-with-resource</h2>
 *
 * <pre>
 * {@code
 *   final Managed<T> m = ...;
 * 
 *   try (final Borrowed<T> b = m.borrow()) {
 *     final T value = b.get();
 * 
 *     // do something with 'value'.
 *   }
 * }
 * </pre>
 *
 * <h2>Example using release</h2>
 *
 * <pre>
 * {@code
 *   final Managed<T> m = ...;
 * 
 *   final Managed.Borrowed<T> b = m.borrow();
 * 
 *   try {
 *     final T value = b.get();
 * 
 *     // do something with 'value'.
 *   } finally {
 *     b.release();
 *   }
 * }
 * </pre>
 *
 * @author udoprog
 *
 * @param <T> The type of the object being managed.
 */
public interface Managed<T> {
    /**
     * System property that if set to 'yes', will cause the managed references to be traced.
     */
    public static final String TRACING = "eu.toolchain.async.Managed.trace";

    /**
     * System property that if set to 'yes', will cause stacks to be captured by borrowed references.
     */
    public static final String CAPTURE_STACK = "eu.toolchain.async.Manabed.captureStack";

    public AsyncFuture<Void> start();

    /**
     * Stop the underlying managed reference. Can be called multiple times.
     *
     * <p>
     * A stop call will do the following (in order).
     * </p>
     * 
     * <ul>
     * <li>future borrowed references are <em>not</em> valid</li>
     * <li>waits for number of references to become zero, this indicates that no one is <em>using</em> the reference</li>
     * <li>destruct the managed reference using {@link ManagedSetup#destruct(Object)}</li>
     * </ul>
     *
     * @return A future that will be resolved when the managed reference is destructed.
     */
    public AsyncFuture<Void> stop();

    /**
     * Borrow the underlying reference.
     *
     * <b>This reference must be explicitly released, otherwise the application could leak reference which will cause
     * {@link #stop()} to misbehave</b>
     *
     * @return A borrowed reference.
     */
    public Borrowed<T> borrow();

    /**
     * Borrow a reference and execute the given action.
     *
     * The reference will be released when the action's future is finished.
     * 
     * @param action The action to perform on the borrowed reference.
     * @return The future returned by the action.
     */
    public <R> AsyncFuture<R> doto(final ManagedAction<T, R> action);

    /**
     * If managed reference is started, but not stopping or stopped.
     *
     * @return {@code true} if the underlying reference is constructed, and available to be borrowed.
     */
    public boolean isReady();
}