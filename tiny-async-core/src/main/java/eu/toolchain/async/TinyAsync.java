package eu.toolchain.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import eu.toolchain.async.caller.DefaultAsyncCaller;
import eu.toolchain.async.caller.ExecutorAsyncCaller;
import eu.toolchain.async.collector.FutureCollector;
import eu.toolchain.async.collector.FutureDiscardCollector;
import eu.toolchain.async.collector.FutureStreamCollector;
import eu.toolchain.async.proxies.LazyTransformCancelledFuture;
import eu.toolchain.async.proxies.LazyTransformErrorFuture;
import eu.toolchain.async.proxies.LazyTransformFuture;

// @formatter:off
/**
 * Entry point to the tiny async framework.
 *
 * <h4>Example usage</h4>
 *
 * {@code
 *   TinyAsync async = TinyAsync.builder().caller(new Slf4jCaller()).build();
 *
 *   final ResolvableFuture<Integer> future = async.future()
 *
 *   someAsyncOperation(future);
 *
 *   future.on(new FutureDone<Integer>() {
 *     void resolved(Integer result) {
 *       // hurray
 *     }
 *
 *     void failed(Throwable cause) {
 *       // nay :(
 *     }
 *   });
 * }
 */
// @formatter:on
public final class TinyAsync implements AsyncFramework {
    @SuppressWarnings("unchecked")
    private static final Collection<Object> EMPTY_RESULTS = Collections.EMPTY_LIST;

    private final ExecutorService callerExecutor;

    /**
     * Default executor to use when resolving asynchronously.
     */
    private final ExecutorService defaultExecutor;

    /**
     * Default set of helper functions for calling callbacks.
     */
    private final AsyncCaller caller;

    private TinyAsync(ExecutorService callerExecutor, ExecutorService defaultExecutor, AsyncCaller caller) {
        this.callerExecutor = callerExecutor;
        this.defaultExecutor = defaultExecutor;
        this.caller = caller;
    }

    /**
     * Fetch the configured primary executor (if any).
     *
     * @return The configured primary executor.
     * @throws IllegalStateException if no caller executor is available.
     */
    public ExecutorService defaultExecutor() {
        if (defaultExecutor == null)
            throw new IllegalStateException("no default executor configured");

        return defaultExecutor;
    }

    /**
     * Fetch the configured callerExecutor (if any).
     *
     * @return The configured callerExecutor.
     * @throws IllegalStateException if no caller executor is available.
     */
    public ExecutorService callerExecutor() {
        if (callerExecutor == null)
            throw new IllegalStateException("no default executor configured");

        return callerExecutor;
    }

    @Override
    public AsyncCaller threadedCaller() {
        if (caller.isThreaded())
            return caller;

        return new ExecutorAsyncCaller(callerExecutor(), caller);
    }

    @Override
    public AsyncCaller caller() {
        return caller;
    }

    @Override
    public <C, T> AsyncFuture<T> transform(final AsyncFuture<C> future,
            final Transform<? super C, ? extends T> transform) {
        final ResolvableFuture<T> target = future();

        future.on(new FutureDone<C>() {
            @Override
            public void failed(Throwable cause) throws Exception {
                target.fail(cause);
            }

            @Override
            public void resolved(C result) throws Exception {
                final T value;

                try {
                    value = transform.transform(result);
                } catch (Exception e) {
                    target.fail(new TransformException(e));
                    return;
                }

                target.resolve(value);
            }

            @Override
            public void cancelled() throws Exception {
                target.cancel();
            }
        });

        return target.bind(future);
    }

    @Override
    public <C, T> AsyncFuture<T> transform(AsyncFuture<C> future, LazyTransform<? super C, ? extends T> transform) {
        final ResolvableFuture<T> target = future();
        future.on(new LazyTransformFuture<C, T>(transform, target));
        return target;
    }

    @Override
    public <T> AsyncFuture<T> error(final AsyncFuture<T> future, final Transform<Throwable, ? extends T> transform) {
        final ResolvableFuture<T> target = future();

        future.on(new FutureDone<T>() {
            @Override
            public void failed(Throwable cause) throws Exception {
                final T value;

                try {
                    value = transform.transform(cause);
                } catch (Exception e) {
                    target.fail(new TransformException(e));
                    return;
                }

                target.resolve(value);
            }

            @Override
            public void resolved(T result) throws Exception {
                target.resolve(result);
            }

            @Override
            public void cancelled() throws Exception {
                target.cancel();
            }
        });

        return target.bind(future);
    }

    @Override
    public <T> AsyncFuture<T> error(final AsyncFuture<T> future, final LazyTransform<Throwable, ? extends T> transform) {
        final ResolvableFuture<T> target = future();
        future.on(new LazyTransformErrorFuture<T>(transform, target));
        return target;
    }

    @Override
    public <T> AsyncFuture<T> cancelled(final AsyncFuture<T> future, final Transform<Void, ? extends T> transform) {
        final ResolvableFuture<T> target = future();

        future.on(new FutureDone<T>() {
            @Override
            public void failed(Throwable cause) throws Exception {
                target.fail(cause);
            }

            @Override
            public void resolved(T result) throws Exception {
                target.resolve(result);
            }

            @Override
            public void cancelled() throws Exception {
                final T value;

                try {
                    value = transform.transform(null);
                } catch (Exception e) {
                    target.fail(new TransformException(e));
                    return;
                }

                target.resolve(value);
            }
        });

        return target.bind(future);
    }

    @Override
    public <T> AsyncFuture<T> cancelled(final AsyncFuture<T> future, final LazyTransform<Void, ? extends T> transform) {
        final ResolvableFuture<T> target = future();
        future.on(new LazyTransformCancelledFuture<T>(transform, target));
        return target;
    }

    @Override
    public <C> AsyncFuture<C> call(final Callable<? extends C> callable) {
        return call(callable, defaultExecutor(), this.<C> future());
    }

    @Override
    public <C> AsyncFuture<C> call(final Callable<? extends C> callable, final ExecutorService executor) {
        return call(callable, executor, this.<C> future());
    }

    @Override
    public <C> AsyncFuture<C> call(final Callable<? extends C> callable, final ExecutorService executor,
            final ResolvableFuture<C> future) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // future is already done, do not perform potentially expensive operation.
                if (future.isDone())
                    return;

                final C result;

                try {
                    result = callable.call();
                } catch (final Exception error) {
                    future.fail(error);
                    return;
                }

                future.resolve(result);
            }
        };

        final Future<?> task;

        try {
            task = executor.submit(runnable);
        } catch (final Exception e) {
            future.fail(e);
            return future;
        }

        future.on(new FutureCancelled() {
            @Override
            public void cancelled() throws Exception {
                // cancel, but do not interrupt.
                task.cancel(false);
            }
        });

        return future;
    }

    @Override
    public <T> ResolvableFuture<T> future() {
        return new ConcurrentFuture<T>(this, caller);
    }

    @Override
    public AsyncFuture<Void> resolved() {
        return resolved(null);
    }

    @Override
    public <T> AsyncFuture<T> resolved(T value) {
        return new ResolvedFuture<T>(this, caller, value);
    }

    @Override
    public <T> AsyncFuture<T> failed(Throwable e) {
        return new FailedFuture<T>(this, caller, e);
    }

    @Override
    public <T> AsyncFuture<T> cancelled() {
        return new CancelledFuture<T>(this, caller);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> AsyncFuture<Collection<T>> collect(final Collection<? extends AsyncFuture<? extends T>> futures) {
        if (futures.isEmpty())
            return resolved((Collection<T>) EMPTY_RESULTS);

        final ResolvableFuture<Collection<T>> target = future();

        final FutureDone<T> done = new FutureCollector<T, Collection<T>>(futures.size(), Collectors.<T> collection(),
                target);

        for (final AsyncFuture<? extends T> q : futures)
            q.on(done);

        bindSignals(target, futures);
        return target;
    }

    @Override
    public <C, T> AsyncFuture<T> collect(final Collection<? extends AsyncFuture<? extends C>> futures,
            final Collector<? super C, ? extends T> collector) {
        if (futures.isEmpty())
            return this.collectEmpty(collector);

        final ResolvableFuture<T> target = future();

        final FutureCollector<? super C, ? extends T> done = new FutureCollector<>(futures.size(), collector, target);

        for (final AsyncFuture<? extends C> q : futures)
            q.on(done);

        bindSignals(target, futures);
        return target;
    }

    /**
     * Shortcut for when the list of futures is empty.
     */
    @SuppressWarnings("unchecked")
    private <C, T> AsyncFuture<T> collectEmpty(final Collector<C, ? extends T> collector) {
        try {
            return this.<T> resolved(collector.collect((Collection<C>) EMPTY_RESULTS));
        } catch (Exception e) {
            return failed(e);
        }
    }

    @Override
    public <C, T> AsyncFuture<T> collect(final Collection<? extends AsyncFuture<? extends C>> futures,
            final StreamCollector<? super C, ? extends T> collector) {
        if (futures.isEmpty())
            return collectEmpty(collector);

        final ResolvableFuture<T> target = future();

        final FutureStreamCollector<? super C, ? extends T> done = new FutureStreamCollector<>(caller, futures.size(),
                collector, target);

        for (final AsyncFuture<? extends C> q : futures)
            q.on(done);

        bindSignals(target, futures);
        return target;
    }

    @Override
    public <C, T> AsyncFuture<T> eventuallyCollect(
            final Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables,
            final StreamCollector<? super C, ? extends T> collector, int parallelism) {
        if (callables.isEmpty()) {
            final T value;

            try {
                value = collector.end(0, 0, 0);
            } catch (Exception e) {
                return failed(e);
            }

            return resolved(value);
        }

        // Special case: the specified parallelism is sufficient to run all at once.
        if (parallelism >= callables.size())
            return delayedCollectParallel(callables, collector);

        final TinySemaphore mutex = new TinySemaphoreImpl(parallelism);
        final ResolvableFuture<T> future = future();
        defaultExecutor().execute(
                new DelayedCollectCoordinator<>(caller, callables, collector, mutex, future, parallelism));
        return future;
    }

    private <C, T> AsyncFuture<T> delayedCollectParallel(
            Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables,
            StreamCollector<? super C, ? extends T> collector) {
        final List<AsyncFuture<? extends C>> futures = new ArrayList<>(callables.size());

        for (final Callable<? extends AsyncFuture<? extends C>> c : callables) {
            final AsyncFuture<? extends C> future;

            try {
                future = c.call();
            } catch (Exception e) {
                futures.add(this.<C> failed(e));
                continue;
            }

            futures.add(future);
        }

        return collect(futures, collector);
    }

    /**
     * Bind the given collection of futures to the target future, which if cancelled, or failed will do the
     * corresponding to their collection of futures.
     *
     * @param target The future to cancel, and fail on.
     * @param futures The futures to cancel, when {@code target} is cancelled.
     */
    private <T> void bindSignals(final AsyncFuture<T> target, final Collection<? extends AsyncFuture<?>> futures) {
        target.on(new FutureCancelled() {
            @Override
            public void cancelled() throws Exception {
                for (final AsyncFuture<?> f : futures)
                    f.cancel();
            }
        });
    }

    /**
     * Shortcut for when the list of futures is empty with {@link StreamCollector}.
     */
    private <C, T> AsyncFuture<T> collectEmpty(final StreamCollector<? super C, ? extends T> collector) {
        try {
            return this.<T> resolved(collector.end(0, 0, 0));
        } catch (Exception e) {
            return failed(e);
        }
    }

    @Override
    public <C> AsyncFuture<Void> collectAndDiscard(Collection<? extends AsyncFuture<C>> futures) {
        if (futures.isEmpty())
            return resolved(null);

        final ResolvableFuture<Void> target = future();

        final FutureDone<C> done = new FutureDiscardCollector<>(futures.size(), target);

        for (final AsyncFuture<C> q : futures)
            q.on(done);

        return target;
    }

    @Override
    public <C> Managed<C> managed(ManagedSetup<C> setup) {
        return new TinyManaged<C>(this, setup);
    }

    /**
     * Build a new TinyAsync instance.
     *
     * @return A builder for the TinyAsync instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AsyncCaller caller = new DefaultAsyncCaller();
        private boolean threaded = false;
        private ExecutorService defaultExecutor = null;
        private ExecutorService callerExecutor = null;

        private Builder() {
        }

        /**
         * Configure that all caller invocation, and async tasks should be using a thread pool.
         *
         * This will cause the configuration of TinyTask to throw an exception if an executor service is not available
         * for all purposes.
         *
         * @param threaded Set {@code true} if all tasks should be executed on a thread pool, {@code false} otherwise.
         * @return This builder.
         */
        public Builder threaded(boolean threaded) {
            this.threaded = threaded;
            return this;
        }

        /**
         * Specify an asynchronous caller implementation.
         *
         * The 'caller' defines how handles are invoked. The simplest implementations are based of
         * {@code DirectAsyncCaller}, which causes the call to be performed directly in the calling thread.
         *
         *
         * @param caller
         * @return
         * @see
         */
        public Builder caller(AsyncCaller caller) {
            if (caller == null)
                throw new IllegalArgumentException("caller");

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
        public Builder executor(ExecutorService defaultExecutor) {
            if (defaultExecutor == null)
                throw new IllegalArgumentException("defaultExecutor");

            this.defaultExecutor = defaultExecutor;
            return this;
        }

        /**
         * Specify a separate executor to use for caller (internal handle) invocation.
         *
         * @param callerExecutor Executor to use for callers.
         * @return This builder.
         */
        public Builder callerExecutor(ExecutorService callerExecutor) {
            if (callerExecutor == null)
                throw new IllegalArgumentException("callerExecutor");

            this.callerExecutor = callerExecutor;
            return this;
        }

        public TinyAsync build() {
            final ExecutorService defaultExecutor = setupDefaultExecutor();
            final ExecutorService callerExecutor = setupCallerExecutor(defaultExecutor);
            final AsyncCaller caller = setupCaller(callerExecutor);

            return new TinyAsync(callerExecutor, defaultExecutor, caller);
        }

        private ExecutorService setupDefaultExecutor() {
            if (defaultExecutor != null)
                return defaultExecutor;

            if (threaded)
                throw new IllegalStateException("no primary executor service available, set one using "
                        + "either #executor(ExecutorService))");

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

            if (threaded)
                throw new IllegalStateException("no executor service available for caller, set one using "
                        + "either #executor(ExecutorService) or #callerExecutor(ExecutorService)");

            return null;
        }

        /**
         * If a threaded caller is requested (through {@code #threaded(boolean)}), asserts that the provided caller uses
         * threads.
         *
         * @return A caller implementation according to the provided configuration.
         */
        private AsyncCaller setupCaller(ExecutorService callerExecutor) {
            if (threaded && callerExecutor == null)
                throw new IllegalStateException("no executor service available for caller, set one using "
                        + "either #executor(ExecutorService) or #callerExecutor(ExecutorService)");

            if (caller == null)
                throw new IllegalStateException("caller: must be configured");

            if (threaded) {
                if (this.caller.isThreaded())
                    return caller;

                return new ExecutorAsyncCaller(callerExecutor, caller);
            }

            return caller;
        }
    }
}
