package eu.toolchain.concurrent;

import java.util.function.Function;

/**
 * Managed lightweight, reference-counted objects.
 *
 * <p>This utility class guarantees that the underlying reference has been initialized and that the
 * block of code using it will never operate on an invalid reference.
 *
 * <p>An invalid reference is an object that has been freed or destructed before it is being used.
 * An example would be a database connection that has been closed, any subsequent actions on it is
 * very likely to cause an exception to be thrown. By wrapping the connection in a managed
 * reference, any block of code using the reference prevents it from being deconstructed until it is
 * no longer being used.
 *
 * <p>Since we cannot rely on the garbage collector being invoked in a timely fashion, the borrowed
 * references has to be manually released. Fortunately, there are some helpers to ease this like
 * {@link #doto(Function)}, the try-with-resources pattern on a {@link Borrowed} reference, and the
 * {@link Borrowed#release()} method.
 *
 * <p>This pattern is useful if you have a block of code returning a future.
 *
 * It guarantees that the code is executed in a safe fashion, that will release the reference if it
 * throws an exception. After it has successfully returned a future, the borrowed reference will be
 * relased when this future is completed:
 *
 * <pre>{@code
 *   final Managed<T> m = ...;
 *
 *   final Stage<Integer> future = m.doto(new ManagedAction<T, Integer>() {
 *     Stage<Integer> action(final T value) {
 *       // do something with value
 *
 *       return async.completed(42);
 *     }
 *   });
 *
 *   try (final Borrowed<T> b = m.borrow()) {
 *     final T value = b.get();
 *
 *     // do something with 'value'.
 *   }
 * }</pre>
 *
 * <p>The following is an example using try-with-resource:
 *
 * <pre>{@code
 *   final Managed<T> m = ...;
 *
 *   try (final Borrowed<T> b = m.borrow()) {
 *     final T value = b.get();
 *
 *     // do something with 'value'.
 *   }
 * }</pre>
 *
 * <p>Finally, this is how you manually borrow and release the reference after use:
 *
 * <pre>{@code
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
 * }</pre>
 *
 * @param <T> type of the managed reference
 * @author udoprog
 */
public interface Managed<T> {
  /**
   * System property that if set to 'yes', will cause the managed references to be traced.
   */
  String TRACING = Managed.class.getCanonicalName() + ".trace";

  /**
   * System property that if set to 'yes', will cause stacks to be captured by borrowed
   * references.
   */
  String CAPTURE_STACK = Managed.class.getCanonicalName() + ".captureStack";

  /**
   * Start the managed reference.
   *
   * @return a future associated with the start action
   */
  Stage<Void> start();

  /**
   * Stop the underlying managed reference.
   *
   * <p>If called multiple times will only A stop call will do the following (in order):<ul>
   *
   * <li>future borrowed references are <em>not</em> valid</li>
   *
   * <li>waits for the reference count to become zero, this indicates that no one is
   * <em>using</em> the reference</li>
   *
   * <li>destruct the managed reference</li></ul>
   *
   * @return a future associated with the stop action
   */
  Stage<Void> stop();

  /**
   * Borrow the underlying reference.
   *
   * <p><b>This reference must be explicitly released, otherwise the application could leak
   * reference which will cause {@link #stop()} to misbehave</b>
   *
   * @return A borrowed reference.
   */
  Borrowed<T> borrow();

  /**
   * Borrow a reference and execute the given action.
   *
   * <p>The reference will be released when the action's future is finished:<pre>
   * {@code
   *   final Managed<Database> m = ...;
   *
   *   return m.doto(db -> {
   *     return db.query(...);
   *   });
   * }
   * </pre>
   *
   * @param action The action to perform on the borrowed reference.
   * @param <U> the type of the return value from the action
   * @return The future returned by the action.
   */
  <U> Stage<U> doto(
      Function<? super T, ? extends Stage<U>> action
  );

  /**
   * If managed reference is started, but not stopping or stopped.
   *
   * <p><em>Should only be used for diagnostical purposes.</em> Code using references should utilize
   * futures returned by methods like {@link #start()} and {@link #stop()} to know when there
   * reference is ready.
   *
   * @return {@code true} if the underlying reference is constructed, and available to be borrowed.
   */
  boolean isReady();
}
