package eu.toolchain.async.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import lombok.AllArgsConstructor;
import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.Transform;

// @formatter:off
/**
 * A class implementing the callback pattern concurrently in a way that any thread can use the callback instance in a
 * safe manner.
 *
 * The callback uses the calling thread to execute result listeners, see {@link #resolve(Object)}, and {@link #on(FutureDone)} for details.
 *
 * It also allows for cancellation in any order.
 *
 * <h1>Example</h1>
 *
 * <pre>
 * {@code
 *     Callback<Integer> callback = new ConcurrentCallback<Integer>();
 *     new Thread(new Runnable() callback.resolve(12); }).start();
 *     callback.listen(new Callback.Handle<T>() { ... });
 * }
 * </pre>
 *
 * @author udoprog
 *
 * @param <T>
 *            The type being deferred.
 */
// @formatter:on
public class ConcurrentResolvableFuture<T> extends AbstractImmediateAsyncFuture<T> implements ResolvableFuture<T> {
    // waiting for value.
    public static final int RUNNING = 0x0;
    public static final int RESULT_UPDATING = 0x1;

    /* valid end state, are required to be greater than RESULT_UPDATING, otherwise {@link isStateReady(int)} will fail. */
    public static final int RESOLVED = 0x10;
    public static final int FAILED = 0x11;
    public static final int CANCELLED = 0x12;

    private final Object $lock = new Object();

    private final Sync sync;

    /* if callbacks has been executed or not */
    boolean executed = false;
    /* a linked list of callbacks to execute */
    RunnablePair callbacks = null;

    private final AsyncCaller caller;

    /**
     * Setup a concurrent future that uses a custom caller implementation.
     *
     * The provided caller implementation will be called from the calling thread of {@link #on}, or other public methods
     * interacting with this future.
     *
     * It is therefore suggested to provide an implementation that supports delegating tasks to a separate thread pool.
     *
     * @param async The async implementation to use.
     * @param caller The caller implementation to use.
     */
    public ConcurrentResolvableFuture(final AsyncFramework async, final AsyncCaller caller) {
        this(async, caller, new Sync());
    }

    protected ConcurrentResolvableFuture(final AsyncFramework async, final AsyncCaller caller, final Sync sync) {
        super(async);
        this.caller = caller;
        this.sync = sync;
    }

    /* transition */

    @Override
    public boolean resolve(T result) {
        if (!sync.setResult(RESOLVED, result))
            return false;

        run();
        return true;
    }

    @Override
    public boolean fail(Throwable cause) {
        if (!sync.setResult(FAILED, cause))
            return false;

        run();
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!sync.setResult(CANCELLED))
            return false;

        run();
        return true;
    }

    @Override
    public boolean cancel() {
        return cancel(false);
    }

    /* listeners */

    @Override
    public AsyncFuture<T> bind(final AsyncFuture<?> other) {
        final Runnable runnable = otherRunnable(other);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    @Override
    public AsyncFuture<T> on(final FutureDone<? super T> done) {
        final Runnable runnable = doneRunnable(done);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    @Override
    public AsyncFuture<T> on(final FutureCancelled cancelled) {
        final Runnable runnable = cancelledRunnable(cancelled);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    @Override
    public AsyncFuture<T> on(final FutureFinished finishable) {
        final Runnable runnable = finishedRunnable(finishable);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    @Override
    public AsyncFuture<T> on(final FutureResolved<? super T> resolved) {
        final Runnable runnable = resolvedRunnable(resolved);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    @Override
    public AsyncFuture<T> on(final FutureFailed failed) {
        final Runnable runnable = failedRunnable(failed);

        if (add(runnable))
            return this;

        runnable.run();
        return this;
    }

    protected Runnable otherRunnable(final AsyncFuture<?> other) {
        return new Runnable() {
            @Override
            public void run() {
                final int state = sync.poll();

                if (state == CANCELLED)
                    other.cancel();
            }
        };
    }

    protected Runnable doneRunnable(final FutureDone<? super T> done) {
        return new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                final int s = sync.poll();

                if (s == FAILED) {
                    caller.fail(done, (Throwable) sync.result);
                    return;
                }

                if (s == CANCELLED) {
                    caller.cancel(done);
                    return;
                }

                caller.resolve(done, (T) sync.result);
            }
        };
    }

    protected Runnable cancelledRunnable(final FutureCancelled cancelled) {
        return new Runnable() {
            @Override
            public void run() {
                int state = sync.poll();

                if (state == CANCELLED)
                    caller.cancel(cancelled);
            }
        };
    }

    protected Runnable finishedRunnable(final FutureFinished finishable) {
        return new Runnable() {
            @Override
            public void run() {
                caller.finish(finishable);
            }
        };
    }

    protected Runnable resolvedRunnable(final FutureResolved<? super T> resolved) {
        return new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                final int state = sync.poll();

                if (state == RESOLVED)
                    caller.resolve(resolved, (T) sync.result);
            }
        };
    }

    protected Runnable failedRunnable(final FutureFailed failed) {
        return new Runnable() {
            @Override
            public void run() {
                final int state = sync.poll();

                if (state == FAILED)
                    caller.fail(failed, (Throwable) sync.result);
            }
        };
    }

    /* check state */

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public boolean isResolved() {
        return sync.isResolved();
    }

    @Override
    public boolean isFailed() {
        return sync.isFailed();
    }

    @Override
    public boolean isCancelled() {
        return sync.isCancelled();
    }

    /* get result */

    @Override
    public Throwable cause() {
        return sync.cause();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return (T) sync.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getNow() throws ExecutionException {
        return (T) sync.getNow();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return (T) sync.get(unit.toNanos(timeout));
    }

    /* transform */

    @SuppressWarnings("unchecked")
    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.transform(this, transform);

        // shortcut

        if (state == CANCELLED)
            return async.cancelled();

        if (state == FAILED)
            return async.failed((Throwable) sync.result);

        return transformResolved(transform, (T) sync.result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> AsyncFuture<C> lazyTransform(final LazyTransform<? super T, C> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.transform(this, transform);

        // shortcut

        if (state == CANCELLED)
            return async.cancelled();

        if (state == FAILED)
            return async.failed((Throwable) sync.result);

        return lazyTransformResolved(transform, (T) sync.result);
    }

    @Override
    public AsyncFuture<T> catchFailed(Transform<Throwable, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.error(this, transform);

        // shortcut
        if (state == FAILED)
            return transformFailed(transform, (Throwable) sync.result);

        return this;
    }

    @Override
    public AsyncFuture<T> lazyCatchFailed(LazyTransform<Throwable, T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.error(this, transform);

        // shortcut
        if (state == FAILED)
            return lazyTransformFailed(transform, (Throwable) sync.result);

        return this;
    }

    @Override
    public AsyncFuture<T> catchCancelled(Transform<Void, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.cancelled(this, transform);

        // shortcut

        if (state == CANCELLED)
            return transformCancelled(transform);

        return this;
    }

    @Override
    public AsyncFuture<T> lazyCatchCancelled(LazyTransform<Void, T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.cancelled(this, transform);

        // shortcut

        if (state == CANCELLED)
            return lazyTransformCancelled(transform);

        return this;
    }

    /**
     * Take and reset all callbacks.
     */
    RunnablePair takeAndClear() {
        final RunnablePair entries;

        synchronized ($lock) {
            if (executed)
                return null;

            executed = true;
            entries = callbacks;
            callbacks = null;
        }

        return entries;
    }

    void run() {
        RunnablePair entries = takeAndClear();

        while (entries != null) {
            entries.runnable.run();
            entries = entries.next;
        }
    }

    /**
     * Attempt to add an event listener to the list of listeners.
     *
     * This implementation uses a spin-lock, where the loop copies the entire list of listeners.
     *
     * @param type Type of callback to be queued up.
     * @param callback Callback to be queued up.
     * @return {@code true} if a task has been queued up, {@code false} otherwise.
     */
    boolean add(Runnable runnable) {
        if (executed)
            return false;

        synchronized ($lock) {
            if (executed)
                return false;

            callbacks = new RunnablePair(runnable, callbacks);
        }

        return true;
    }

    public static boolean isStateReady(int state) {
        return state > RESULT_UPDATING;
    }

    /**
     * A single node in a list of runnables that should be executed when done.
     */
    @AllArgsConstructor
    static class RunnablePair {
        final Runnable runnable;
        final RunnablePair next;
    }

    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5044031197562766649L;

        Object result;

        @Override
        protected int tryAcquireShared(int ignored) {
            return getState() >= RESOLVED ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int state) {
            return true;
        }

        public Throwable cause() {
            if (getState() != FAILED)
                throw new IllegalStateException("future is not in a failed state");

            return (Throwable) result;
        }

        public boolean isCancelled() {
            return getState() == CANCELLED;
        }

        public boolean isFailed() {
            return getState() == FAILED;
        }

        public boolean isResolved() {
            return getState() == RESOLVED;
        }

        public boolean isDone() {
            return getState() > RESULT_UPDATING;
        }

        public Object get() throws ExecutionException, InterruptedException {
            acquireSharedInterruptibly(-1);

            final int s = getState();

            if (s == CANCELLED)
                throw new CancellationException();

            if (s == FAILED)
                throw new ExecutionException((Throwable) result);

            return result;
        }

        public Object get(long nanos) throws ExecutionException, InterruptedException, TimeoutException {
            if (!tryAcquireSharedNanos(-1, nanos))
                throw new TimeoutException();

            final int s = getState();

            if (s == CANCELLED)
                throw new CancellationException();

            if (s == FAILED)
                throw new ExecutionException((Throwable) result);

            return result;
        }

        public Object getNow() throws ExecutionException {
            final int s = getState();

            if (s == CANCELLED)
                throw new CancellationException();

            if (s == FAILED)
                throw new ExecutionException((Throwable) result);

            if (s == RESOLVED)
                return result;

            throw new IllegalStateException("future is not completed");
        }

        /**
         * Same as {@code #complete(int, Object)} but with a null result.
         *
         * @param state The end state to move the synchronizer into.
         * @return {@code true} if the given transition is valid and happened, {@code false} otherwise}.
         * @see #setResult(int, Object)
         */
        public boolean setResult(int state) {
            if (!compareAndSetState(RUNNING, state))
                return false;

            releaseShared(-1);
            return true;
        }

        /**
         * Move the synchronizer state to the given end state.
         *
         * @param state The end state to move the synchronizer into.
         * @param result The result to associate with the end state, if not {@code null} will cause the synchronizer to
         *            go into an intermediate {@code RESULT_UPDATING} state that has to be accounted for. Use
         *            {@link #poll()} to avoid this.
         * @return {@code true} if the given transition is valid and happened, {@code false} otherwise}.
         */
        public boolean setResult(int state, Object result) {
            if (!compareAndSetState(RUNNING, RESULT_UPDATING))
                return false;

            this.result = result;
            setState(state);
            releaseShared(-1);
            return true;
        }

        public int state() {
            return getState();
        }

        /**
         * Poll for the current state, this is guaranteed to never return
         * {@link ConcurrentResolvableFuture#RESULT_UPDATING}.
         *
         * @return The current state of the synchronizer. Never {@link ConcurrentResolvableFuture#RESULT_UPDATING}.
         */
        public int poll() {
            // spin if the current state is changing.
            int s = getState();

            if (s == RESULT_UPDATING) {
                acquireShared(-1);
                return getState();
            }

            return s;
        }
    }
}
