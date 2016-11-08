package eu.toolchain.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class TinyAsyncBuilder {
    private AsyncCaller caller;
    private boolean threaded;
    private boolean useRecursionSafeCaller;
    private ExecutorService executor;
    private ExecutorService callerExecutor;
    private ScheduledExecutorService scheduler;
    private ClockSource clockSource = ClockSource.system();

    protected TinyAsyncBuilder() {
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
    public TinyAsyncBuilder threaded(boolean threaded) {
        this.threaded = threaded;
        return this;
    }

    /**
     * Configure that all caller invocations should use a recursion safe mechanism. In the normal
     * case this doesn't change the behaviour of caller and threadedCaller, but when deep recursion
     * is detected in the current thread the next recursive call is deferred to a separate thread.
     * <p>
     * Recursion is tracked for all threads that call the AsyncCallers.
     * <p>
     * This will make even the non-threaded caller use a thread in the case of deep recursion.
     *
     * @param useRecursionSafeCaller Set {@code true} if all caller invocations should be done with
     *                               a recursion safe mechanism, {@code false} otherwise.
     * @return This builder.
     */
    public TinyAsyncBuilder recursionSafeAsyncCaller(boolean useRecursionSafeCaller) {
        this.useRecursionSafeCaller = useRecursionSafeCaller;
        return this;
    }

    /**
     * Specify an asynchronous caller implementation.
     * <p>
     * The 'caller' defines how handles are invoked. The simplest implementations are based of
     * {@code DirectAsyncCaller} , which causes the call to be performed directly in the calling
     * thread.
     *
     * @param caller
     * @return This builder.
     */
    public TinyAsyncBuilder caller(AsyncCaller caller) {
        if (caller == null) {
            throw new NullPointerException("caller");
        }

        this.caller = caller;
        return this;
    }

    /**
     * Configure the default executor to use for caller invocation,and asynchronous tasks submitted
     * through {@link AsyncFramework#call(Callable)}.
     *
     * @param executor Executor to use.
     * @return This builder.
     */
    public TinyAsyncBuilder executor(ExecutorService executor) {
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
    public TinyAsyncBuilder callerExecutor(ExecutorService callerExecutor) {
        if (callerExecutor == null) {
            throw new NullPointerException("callerExecutor");
        }

        this.threaded = true;
        this.callerExecutor = callerExecutor;
        return this;
    }

    /**
     * Specify a scheduler to use with the built TinyAsync instance.
     *
     * @param scheduler The scheduler to use.
     * @return This builder.
     */
    public TinyAsyncBuilder scheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public TinyAsyncBuilder clockSource(ClockSource clockSource) {
        if (clockSource == null) {
            throw new NullPointerException("clockSource");
        }

        this.clockSource = clockSource;
        return this;
    }

    public TinyAsync build() {
        final ExecutorService defaultExecutor = setupDefaultExecutor();
        final ExecutorService callerExecutor = setupCallerExecutor(defaultExecutor);
        final AsyncCaller caller = setupCaller();
        final AsyncCaller threadedCaller = setupThreadedCaller(caller, callerExecutor);

        return new TinyAsync(defaultExecutor, caller, threadedCaller, scheduler, clockSource);
    }

    private AsyncCaller setupThreadedCaller(AsyncCaller caller, ExecutorService callerExecutor) {
        if (caller.isThreaded()) {
            return caller;
        }

        if (callerExecutor == null) {
            return null;
        }

        AsyncCaller threadedCaller = new ExecutorAsyncCaller(callerExecutor, caller);

        if (useRecursionSafeCaller) {
            threadedCaller = new RecursionSafeAsyncCaller(callerExecutor, threadedCaller);
        }

        return threadedCaller;
    }

    private ExecutorService setupDefaultExecutor() {
        if (executor != null) {
            return executor;
        }

        return null;
    }

    /**
     * Attempt to setup a caller executor according to the provided implementation.
     *
     * @param defaultExecutor
     * @return
     */
    private ExecutorService setupCallerExecutor(ExecutorService defaultExecutor) {
        if (callerExecutor != null) {
            return callerExecutor;
        }

        if (defaultExecutor != null) {
            return defaultExecutor;
        }

        if (!threaded) {
            return null;
        }

        throw new IllegalStateException("no executor available for caller, set one using " +
            "either #executor(ExecutorService) or #callerExecutor(ExecutorService)");
    }

    /**
     * If a threaded caller is requested (through {@code #threaded(boolean)}), asserts that the
     * provided caller uses threads.
     *
     * @return A caller implementation according to the provided configuration.
     */
    private AsyncCaller setupCaller() {
        if (caller != null) {
            if (useRecursionSafeCaller && callerExecutor != null) {
                // Wrap user supplied AsyncCaller
                return new RecursionSafeAsyncCaller(callerExecutor, caller);
            }
            return caller;
        }

        AsyncCaller newCaller = new PrintStreamDefaultAsyncCaller(
                System.err, Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r);
                thread.setName("tiny-async-deferrer");
                return thread;
            }
        }));

        if (useRecursionSafeCaller && callerExecutor != null) {
            newCaller = new RecursionSafeAsyncCaller(callerExecutor, newCaller);
        }

        return newCaller;
    }
}
