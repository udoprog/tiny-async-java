package eu.toolchain.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class TinyFutureBuilder {
  private FutureCaller caller;
  private boolean threaded;
  private boolean recursionSafe;
  private long maxRecursionDepth = 100;
  private ExecutorService executor;
  private ExecutorService callerExecutor;
  private ScheduledExecutorService scheduler;
  private ClockSource clockSource = ClockSource.system();

  protected TinyFutureBuilder() {
  }

  /**
   * Configure that all caller invocation, and async tasks should be using a thread pool.
   * <p>
   * This will cause the configuration of TinyTask to throw an exception if an executor service is
   * not available for all purposes.
   *
   * @param threaded Set {@code true} if all tasks should be executed on a thread pool, {@code
   * false} otherwise.
   * @return This builder.
   */
  public TinyFutureBuilder threaded(final boolean threaded) {
    this.threaded = threaded;
    return this;
  }

  /**
   * Configure that all caller invocations should use a recursion safe mechanism. In the normal
   * case this doesn't change the behaviour of caller and threadedCaller, but when deep recursion
   * is detected in the current thread the next recursive doCall is deferred to a separate thread.
   * <p>
   * Recursion is tracked for all threads that doCall the AsyncCallers.
   * <p>
   * This will make even the non-threaded caller use a thread in the case of deep recursion.
   *
   * @param recursionSafe Set {@code true} if all caller invocations should be done with a recursion
   * safe mechanism, {@code false} otherwise.
   * @return This builder.
   */
  public TinyFutureBuilder recursionSafe(final boolean recursionSafe) {
    this.recursionSafe = recursionSafe;
    return this;
  }

  /**
   * Configure how many recursions should be allowed.
   * <p>
   * This implies enabling {@link #recursionSafe}.
   *
   * @param maxRecursionDepth The max number of times that a caller may go through {@link
   * FutureCaller} in a single thread.
   * @return This builder.
   */
  public TinyFutureBuilder maxRecursionDepth(final long maxRecursionDepth) {
    this.maxRecursionDepth = maxRecursionDepth;
    this.recursionSafe = true;
    return this;
  }

  /**
   * Specify an asynchronous caller implementation.
   * <p>
   * The 'caller' defines how handles are invoked. The simplest implementations are based of
   * {@code DirectFutureCaller} , which causes the doCall to be performed directly in the calling
   * thread.
   *
   * @param caller
   * @return This builder.
   */
  public TinyFutureBuilder caller(final FutureCaller caller) {
    if (caller == null) {
      throw new NullPointerException("caller");
    }

    this.caller = caller;
    return this;
  }

  /**
   * Configure the default executor to use for caller invocation,and asynchronous tasks submitted
   * through {@link FutureFramework#call(Callable)}.
   *
   * @param executor Executor to use.
   * @return This builder.
   */
  public TinyFutureBuilder executor(final ExecutorService executor) {
    if (executor == null) {
      throw new NullPointerException("executor");
    }

    this.executor = executor;
    return this;
  }

  /**
   * Specify a separate executor to use for caller (internal handle) invocation.
   * <p>
   * Implies use of threaded caller.
   *
   * @param callerExecutor Executor to use for callers.
   * @return This builder.
   */
  public TinyFutureBuilder callerExecutor(final ExecutorService callerExecutor) {
    if (callerExecutor == null) {
      throw new NullPointerException("callerExecutor");
    }

    this.threaded = true;
    this.callerExecutor = callerExecutor;
    return this;
  }

  /**
   * Specify a scheduler to use with the built TinyFuture instance.
   *
   * @param scheduler The scheduler to use.
   * @return This builder.
   */
  public TinyFutureBuilder scheduler(final ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
    return this;
  }

  public TinyFutureBuilder clockSource(final ClockSource clockSource) {
    if (clockSource == null) {
      throw new NullPointerException("clockSource");
    }

    this.clockSource = clockSource;
    return this;
  }

  public TinyFuture build() {
    final ExecutorService defaultExecutor = this.executor;
    final ExecutorService callerExecutor = setupCallerExecutor(defaultExecutor);
    final FutureCaller caller = setupCaller(callerExecutor);

    return new TinyFuture(defaultExecutor, scheduler, caller, clockSource);
  }

  /**
   * Attempt to setup a caller executor according to the provided implementation.
   *
   * @param defaultExecutor
   * @return
   */
  private ExecutorService setupCallerExecutor(final ExecutorService defaultExecutor) {
    if (callerExecutor != null) {
      return callerExecutor;
    }

    if (defaultExecutor != null) {
      return defaultExecutor;
    }

    return null;
  }

  /**
   * Setup the future caller.
   *
   * @return A caller implementation according to the provided configuration.
   */
  private FutureCaller setupCaller(final ExecutorService callerExecutor) {
    FutureCaller caller;

    if (this.caller != null) {
      caller = this.caller;
    } else {
      caller = new PrintStreamFutureCaller(System.err);
    }

    if (threaded) {
      if (callerExecutor == null) {
        throw new IllegalStateException("#threaded enabled, but no caller executor configured");
      }

      caller = new ExecutorFutureCaller(callerExecutor, caller);
    }

    if (recursionSafe) {
      if (callerExecutor == null) {
        throw new IllegalStateException(
            "#recursionSafe enabled, but no caller executor configured");
      }

      caller = new RecursionSafeFutureCaller(callerExecutor, caller, maxRecursionDepth);
    }

    return caller;
  }
}
