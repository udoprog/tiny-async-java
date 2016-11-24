/*
 * An AsyncCaller implementation that will try to run the call in the current thread as much as
 * possible, while keeping track of recursion to avoid StackOverflowException in the thread. If
 * recursion becomes too deep, the next call is deferred to a separate thread (normal thread pool).
 * State is kept per-thread - stack overflow will be avoided for any thread that passes this code.
 * It is vital to choose a suitable maximum recursion depth.
 */
package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * A {@link Caller} implementation that implements immediate calling, and provides a fallback to
 * avoid blowing up the stack when operations are recursively called.
 *
 * This implementation maintains a thread-local storage that gives a rough estimate of how deep a
 * given stack is.
 */
public final class RecursionSafeCaller implements Caller {
  private final ExecutorService executorService;
  private final Caller caller;
  private final long maxRecursionDepth;

  private final ThreadLocal<Integer> recursionDepthPerThread = new ThreadLocal<Integer>() {
    protected Integer initialValue() {
      return 0;
    }
  };

  public RecursionSafeCaller(
      ExecutorService executorService, Caller caller, long maxRecursionDepth
  ) {
    this.executorService = executorService;
    this.caller = caller;
    this.maxRecursionDepth = maxRecursionDepth;
  }

  public RecursionSafeCaller(ExecutorService executorService, Caller caller) {
    this(executorService, caller, 100);
  }

  @Override
  public void referenceLeaked(final Object reference, final StackTraceElement[] stack) {
    execute(() -> caller.referenceLeaked(reference, stack));
  }

  @Override
  public void execute(final Runnable runnable) {
    // Use thread local counter for recursionDepth.
    final Integer recursionDepth = recursionDepthPerThread.get();
    // ++
    recursionDepthPerThread.set(recursionDepth + 1);

    if (recursionDepth + 1 <= maxRecursionDepth) {
      /* Case A: Call immediately, this is default until we've reached deep recursion. */
      runnable.run();
    } else {
      /* Case B: Defer to a separate thread.
       * This happens when recursion depth of the current thread is larger than limit, to avoid
       * stack overflow. */
      executorService.submit(runnable);
    }

    // --
    recursionDepthPerThread.set(recursionDepth);
  }
}
