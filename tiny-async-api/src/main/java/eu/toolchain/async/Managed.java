package eu.toolchain.async;

//@formatter:off
/**
* Managed lightweight, reference-counted objects.
*
* This utility class guarantees that the underlying reference has been initialized and that the block of code using it
* will never operate on an invalid reference.
*
* An invalid reference, is an object that has been 'destructed', an example would be a database connection that has
* been closed, any subsequent actions on it is very likely to cause an exception to be thrown. By wrapping the
* connection in a managed reference, any block of code using the reference prevents it from being de-allocated until
* that block of code has exited.
*
* <h4>caveats</h4>
*
* Since we cannot rely on the garbage collector being invoked in a timely fashion, the borrowed references has to be
* manually released. Fortunately, there are some helpers to ease this like {@code #doto(AsyncFramework, ManagedAction)}
* , the try-with-resources pattern on a {@code Borrowed} reference, and the {@code Borrowed#release()} method.
*
* <h5>Example using doto</h5>
*
* This pattern is useful if you have a block of code returning a future.
*
* It guarantees that the code is executed in a safe fashion, that will release the reference if it throws an exception.
* After it has successfully returned a future, the borrowed reference will be relased when this future is resolved.
*
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
*
* <h5>Example using try-with-resource</h5>
*
* {@code
*   final Managed<T> m = ...;
*
*   try (final Borrowed<T> b = m.borrow()) {
*     final T value = b.get();
*
*     // do something with 'value'.
*   }
* }
*
* <h5>Example using release</h5>
*
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
*
* @param <T> The type of the object being managed.
*/
//@formatter:on
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
     * Stop the underlying managed type.
     *
     * Any pending borrowed references will prevent it from being stopped.
     *
     * If a start is currently pending, will block until it is resolved.
     */
    public AsyncFuture<Void> stop();

    /**
     * Borrow a reference.
     *
     * Warning: This reference must be explicitly released.
     */
    public Borrowed<T> borrow();

    /**
     * Borrow a reference and execute the given action.
     *
     * The reference will be released when the action's future is finished.
     */
    public <R> AsyncFuture<R> doto(final ManagedAction<T, R> action);

    /**
     * If managed reference is started, but not stopping or stopped.
     */
    public boolean isReady();
}