package eu.toolchain.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class TinyAsyncBuilder {
    private AsyncCaller caller;
    private boolean threaded;
    private ExecutorService executor;
    private ExecutorService callerExecutor;

    protected TinyAsyncBuilder() {
    }

    /**
     * Configure that all caller invocation, and async tasks should be using a thread pool.
     *
     * This will cause the configuration of TinyTask to throw an exception if an executor service is not available for
     * all purposes.
     *
     * @param threaded Set {@code true} if all tasks should be executed on a thread pool, {@code false} otherwise.
     * @return This builder.
     */
    public TinyAsyncBuilder threaded(boolean threaded) {
        this.threaded = threaded;
        return this;
    }

    /**
     * Specify an asynchronous caller implementation.
     *
     * The 'caller' defines how handles are invoked. The simplest implementations are based of {@code DirectAsyncCaller}
     * , which causes the call to be performed directly in the calling thread.
     *
     *
     * @param caller
     * @return
     * @see
     */
    public TinyAsyncBuilder caller(AsyncCaller caller) {
        if (caller == null)
            throw new NullPointerException("caller");

        this.caller = caller;
        return this;
    }

    /**
     * Configure the default executor to use for caller invocation,and asynchronous tasks submitted through
     * {@link AsyncFramework#call(Callable)}.
     *
     * @param executor Executor to use.
     * @return This builder.
     */
    public TinyAsyncBuilder executor(ExecutorService executor) {
        if (executor == null)
            throw new NullPointerException("executor");

        this.executor = executor;
        return this;
    }

    /**
     * Specify a separate executor to use for caller (internal handle) invocation.
     *
     * Implies use of threaded caller.
     *
     * @param callerExecutor Executor to use for callers.
     * @return This builder.
     */
    public TinyAsyncBuilder callerExecutor(ExecutorService callerExecutor) {
        if (callerExecutor == null)
            throw new NullPointerException("callerExecutor");

        this.threaded = true;
        this.callerExecutor = callerExecutor;
        return this;
    }

    public TinyAsync build() {
        final ExecutorService defaultExecutor = setupDefaultExecutor();
        final ExecutorService callerExecutor = setupCallerExecutor(defaultExecutor);
        final AsyncCaller caller = setupCaller();
        final AsyncCaller threadedCaller = setupThreadedCaller(caller, callerExecutor);

        return new TinyAsync(defaultExecutor, caller, threadedCaller);
    }

    private AsyncCaller setupThreadedCaller(AsyncCaller caller, ExecutorService callerExecutor) {
        if (caller.isThreaded())
            return caller;

        if (callerExecutor != null)
            return new ExecutorAsyncCaller(callerExecutor, caller);

        return null;
    }

    private ExecutorService setupDefaultExecutor() {
        if (executor != null)
            return executor;

        return null;
    }

    /**
     * Attempt to setup a caller executor according to the provided implementation.
     *
     * @param defaultExecutor
     * @return
     */
    private ExecutorService setupCallerExecutor(ExecutorService defaultExecutor) {
        if (callerExecutor != null)
            return callerExecutor;

        if (defaultExecutor != null)
            return defaultExecutor;

        if (!threaded)
            return null;

        throw new IllegalStateException("no executor available for caller, set one using "
                + "either #executor(ExecutorService) or #callerExecutor(ExecutorService)");
    }

    /**
     * If a threaded caller is requested (through {@code #threaded(boolean)}), asserts that the provided caller uses
     * threads.
     *
     * @return A caller implementation according to the provided configuration.
     */
    private AsyncCaller setupCaller() {
        if (caller == null)
            return new StderrDefaultAsyncCaller();

        return caller;
    }
}