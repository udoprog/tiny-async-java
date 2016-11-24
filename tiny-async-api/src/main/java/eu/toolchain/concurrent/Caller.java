package eu.toolchain.concurrent;

/**
 * User-defined methods for calling user-defined actions and report errors.
 *
 * <p>Any time a future needs to invoke user-specified code the call is wrapped in
 * {@link #execute(Runnable)}. This allows the user to implement policies and safe guard for how
 * these interactions should happen by providing their own caller implementation.
 *
 * <p>The implementation of these methods will be invoked from the calling thread that interacts
 * with the future.
 *
 * <p>The core of the framework provides some base classes for easily accomplishing this, most
 * notable is {@code DirectCaller}.
 */
public interface Caller {
  /**
   * Indicate that a Managed reference has been leaked.
   *
   * <p>This is usually called by end-user code that never releases a managed reference, like this:
   *
   * <pre>{@code
   * public static class Example {
   *   private final Async async;
   *   private final Managed<Database> database;
   *
   *   public Example(final Async async, final Managed<Database> database) {
   *     this.async = async;
   *     this.database = database;
   *   }
   *
   *   public Stage<Void> doSomething() {
   *     return database.doto(database -> {
   *       Completable<Void> stage = async.future();
   *       return stage;
   *     });
   *   }
   * }
   * }</pre>
   *
   * <p>This leaves the managed database in an open state since its reference count will never go
   * back to zero. When the future and the corresponding borrowed is garbage collected, it will be
   * reported here.
   *
   * @param reference the reference that was leaked
   * @param stack the stacktrace for where it was leaked, can be {@code null} if the information is
   * unavailable
   */
  void referenceLeaked(Object reference, StackTraceElement[] stack);

  /**
   * Execute the given action.
   *
   * @param runnable action to execute
   */
  void execute(Runnable runnable);
}
