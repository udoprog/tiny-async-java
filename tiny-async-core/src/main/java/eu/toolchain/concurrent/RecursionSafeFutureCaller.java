/*
 * An AsyncCaller implementation that will try to run the call in the current thread as much as
 * possible, while keeping track of recursion to avoid StackOverflowException in the thread. If
 * recursion becomes too deep, the next call is deferred to a separate thread (normal thread pool).
 * State is kept per-thread - stack overflow will be avoided for any thread that passes this code.
 * It is vital to choose a suitable maximum recursion depth.
 */
package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public final class RecursionSafeFutureCaller implements FutureCaller {
  private final ExecutorService executorService;
  private final FutureCaller caller;
  private final long maxRecursionDepth;

  private final ThreadLocal<Integer> recursionDepthPerThread = new ThreadLocal<Integer>() {
    protected Integer initialValue() {
      return 0;
    }
  };

  public RecursionSafeFutureCaller(
      ExecutorService executorService, FutureCaller caller, long maxRecursionDepth
  ) {
    this.executorService = executorService;
    this.caller = caller;
    this.maxRecursionDepth = maxRecursionDepth;
  }

  public RecursionSafeFutureCaller(ExecutorService executorService, FutureCaller caller) {
    this(executorService, caller, 100);
  }

  @Override
  public <T> void resolve(final CompletionHandle<T> handle, final T result) {
    execute(() -> caller.resolve(handle, result));
  }

  @Override
  public <T> void fail(final CompletionHandle<T> handle, final Throwable error) {
    execute(() -> caller.fail(handle, error));
  }

  @Override
  public <T> void cancel(final CompletionHandle<T> handle) {
    execute(() -> caller.cancel(handle));
  }

  @Override
  public void cancel(final Runnable cancelled) {
    execute(() -> caller.cancel(cancelled));
  }

  @Override
  public void finish(final Runnable finishable) {
    execute(() -> caller.finish(finishable));
  }

  @Override
  public <T> void resolve(final Consumer<T> resolved, final T value) {
    execute(() -> caller.resolve(resolved, value));
  }

  @Override
  public <T, R> void resolve(final StreamCollector<T, R> collector, final T result) {
    execute(() -> caller.resolve(collector, result));
  }

  @Override
  public <T, R> void fail(final StreamCollector<T, R> collector, final Throwable error) {
    execute(() -> caller.fail(collector, error));
  }

  @Override
  public <T, R> void cancel(final StreamCollector<T, R> collector) {
    execute(() -> caller.cancel(collector));
  }

  @Override
  public void fail(final Consumer<? super Throwable> failed, final Throwable cause) {
    execute(() -> caller.fail(failed, cause));
  }

  @Override
  public <T> void referenceLeaked(final T reference, final StackTraceElement[] stack) {
    execute(() -> caller.referenceLeaked(reference, stack));
  }

  void execute(final Runnable runnable) {
    // Use thread local counter for recursionDepth
    final Integer recursionDepth = recursionDepthPerThread.get();
    // ++
    recursionDepthPerThread.set(recursionDepth + 1);

    if (recursionDepth + 1 <= maxRecursionDepth) {
      // Case A: Call immediately, this is default until we've reached deep recursion
      runnable.run();
    } else {
            /*
             * Case B: Defer to a separate thread
             * This happens when recursion depth of the current thread is larger than limit, to
             * avoid stack overflow.
             */
      executorService.submit(runnable);
    }

    // --
    recursionDepthPerThread.set(recursionDepth);
  }
}
