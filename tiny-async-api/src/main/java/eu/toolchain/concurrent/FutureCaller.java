package eu.toolchain.concurrent;

import java.util.function.Consumer;

/**
 * User-defined functions to thenHandle unexpected circumstances.
 *
 * <p>The implementation of these methods will be invoked from the calling thread that interacts with
 * the future.
 *
 * <p>None of the below methods throw checked exceptions, and they are intended to never throw
 * anything, with the exception of {@code Error}. This means that the implementor is required to
 * make sure this doesn't happen, the best way to accomplish this is to wrap each callback in a
 * try-catch statement like this:
 *
 * <pre>{@code
 * new FutureCaller() {
 *   public <T> void complete(CompletionHandle<T> thenHandle, T result) {
 *     try {
 *       thenHandle.completed(result);
 *     } catch(Exception e) {
 *       // log unexpected error
 *     }
 *   }
 *
 *   // .. other methods
 * }
 * }</pre>
 *
 * <p>The core of the framework provides some base classes for easily accomplishing this, most
 * notable is {@code DirectAsyncCaller}.
 *
 * @author udoprog
 */
public interface FutureCaller {
  /**
   * Indicate that a Managed reference has been leaked.
   *
   * @param reference The reference that was leaked
   * @param stack The stacktrace for where it was leaked, can be {@code null} if information is
   * unavailable
   */
  void referenceLeaked(Object reference, StackTraceElement[] stack);

  /**
   * Execute the given runnable.
   *
   * @param runnable Runnable to execute.
   */
  void execute(final Runnable runnable);
}
