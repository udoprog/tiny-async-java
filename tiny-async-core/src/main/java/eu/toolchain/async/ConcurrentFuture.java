package eu.toolchain.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import lombok.AllArgsConstructor;

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
public class ConcurrentFuture<T> implements ResolvableFuture<T>, FutureDone<T> {
    /**
     * Maximum amount of spins this future allows when trying to modify the callback list.
     *
     * When reached it indicates high contention.
     */
    private static final int MAX_SPINS = 10;

    private AtomicReference<List<CallbackEntry<T>>> callbacks = new AtomicReference<List<CallbackEntry<T>>>(
            new ArrayList<CallbackEntry<T>>());
    private Sync result = new Sync();

    private final AsyncFramework async;
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
    public ConcurrentFuture(final AsyncFramework async, final AsyncCaller caller) {
        this.async = async;
        this.caller = caller;
    }

    @Override
    public void failed(Throwable cause) throws Exception {
        fail(cause);
    }

    @Override
    public void resolved(T result) throws Exception {
        resolve(result);
    }

    @Override
    public void cancelled() throws Exception {
        cancel();
    }

    /* transition */

    @Override
    public boolean resolve(T result) {
        if (!this.result.complete(Sync.RESOLVED, result))
            return false;

        for (final CallbackEntry<T> c : callbacks.getAndSet(null))
            c.resolved(caller, result);

        return true;
    }

    @Override
    public boolean fail(Throwable cause) {
        if (!result.complete(Sync.FAILED, cause))
            return false;

        for (final CallbackEntry<T> c : callbacks.getAndSet(null))
            c.failed(caller, cause);

        return true;
    }

    @Override
    public boolean cancel() {
        return cancel(false);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!result.complete(Sync.CANCELLED, null))
            return false;

        for (final CallbackEntry<T> c : callbacks.getAndSet(null))
            c.cancelled(caller);

        return true;
    }

    /* listeners */

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> on(final FutureDone<? super T> done) {
        if (add(CallbackEntry.DONE, done))
            return this;

        final int state = this.result.state();
        final Object result = this.result.result();

        if (state == Sync.RUNNING)
            throw new IllegalStateException("result is not available");

        if (state == Sync.RESOLVED) {
            caller.resolveFutureDone(done, (T) result);
            return this;
        }

        if (state == Sync.FAILED) {
            caller.failFutureDone(done, (Throwable) result);
            return this;
        }

        throw new IllegalStateException("invalid result state: " + state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> onAny(FutureDone<?> handle) {
        return on((FutureDone<T>) handle);
    }

    @Override
    public AsyncFuture<T> on(FutureCancelled cancelled) {
        if (add(CallbackEntry.CANCELLED, cancelled))
            return this;

        caller.runFutureCancelled(cancelled);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        if (add(CallbackEntry.FINISHED, finishable))
            return this;

        caller.runFutureFinished(finishable);
        return this;
    }

    /* check state */

    @Override
    public boolean isDone() {
        return result.state() > Sync.RESULT;
    }

    @Override
    public boolean isCancelled() {
        return result.state() == Sync.CANCELLED;
    }

    /* get result */

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (!isDone())
            result.acquire();

        return checkState();
    }

    @Override
    public T getNow() throws ExecutionException {
        return checkState();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!result.acquire(unit.toNanos(timeout)))
            throw new TimeoutException();

        return checkState();
    }

    @SuppressWarnings("unchecked")
    private T checkState() throws ExecutionException, CancellationException {
        final int state = result.poll();

        if (state <= Sync.RESULT)
            throw new IllegalStateException("result is not ready");

        final Object result = this.result.result();

        switch (state) {
        case Sync.FAILED:
            throw new ExecutionException((Throwable) result);
        case Sync.RESOLVED:
            return (T) result;
        case Sync.CANCELLED:
            throw new CancellationException();
        default:
            throw new IllegalStateException("illegal state: " + state);
        }
    }

    /* transform */

    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        return async.transform(this, transform);
    }

    @Override
    public <C> AsyncFuture<C> transform(final LazyTransform<? super T, ? extends C> transform) {
        return async.transform(this, transform, caller);
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        return async.error(this, transform);
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, ? extends T> transform) {
        return async.error(this, transform, caller);
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        return async.cancelled(this, transform);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, ? extends T> transform) {
        return async.cancelled(this, transform, caller);
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
    private boolean add(byte type, Object callback) {
        // already done.
        if (result.state() > Sync.RESULT)
            return false;

        final CallbackEntry<T> entry = new CallbackEntry<T>(type, callback);

        List<CallbackEntry<T>> old;
        List<CallbackEntry<T>> copy;

        int spins = 0;

        do {
            // try to mitigate high-contention situations by keeping track of how many times we've looped.
            // a yield is not guaranteed to help, but its the best we have.
            if (spins++ > MAX_SPINS) {
                Thread.yield();
                spins = 0;
            }

            old = callbacks.get();

            if (old == null)
                return false;

            copy = new ArrayList<>(old);
            copy.add(entry);
        } while (!callbacks.compareAndSet(old, copy));

        return true;
    }

    @AllArgsConstructor
    private static class CallbackEntry<T> {
        private static final byte DONE = 0x0;
        private static final byte FINISHED = 0x1;
        private static final byte CANCELLED = 0x2;

        /**
         * As sparse type information as possible.
         */
        private final byte type;

        /**
         * The actual callback.
         */
        private final Object callback;

        @SuppressWarnings("unchecked")
        private void resolved(AsyncCaller caller, T result) {
            if (type == DONE) {
                caller.resolveFutureDone((FutureDone<T>) callback, result);
                return;
            }

            if (type == FINISHED) {
                caller.runFutureFinished((FutureFinished) callback);
                return;
            }

            if (type == CANCELLED)
                return;

            throw new IllegalStateException("invalid callback type: " + type);
        }

        @SuppressWarnings("unchecked")
        private void failed(AsyncCaller caller, Throwable error) {
            if (type == DONE) {
                caller.failFutureDone((FutureDone<T>) callback, error);
                return;
            }

            if (type == FINISHED) {
                caller.runFutureFinished((FutureFinished) callback);
                return;
            }

            if (type == CANCELLED)
                return;

            throw new IllegalStateException("invalid callback type: " + type);
        }

        @SuppressWarnings("unchecked")
        private void cancelled(AsyncCaller caller) {
            if (type == DONE) {
                caller.cancelFutureDone((FutureDone<T>) callback);
                return;
            }

            if (type == FINISHED) {
                caller.runFutureFinished((FutureFinished) callback);
                return;
            }

            if (type == CANCELLED) {
                caller.runFutureCancelled((FutureCancelled) callback);
                return;
            }

            throw new IllegalStateException("invalid callback type: " + type);
        }
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        // waiting for value.
        private static final int RUNNING = 0x0;
        // not running, but result not set yet.
        private static final int RESULT = 0x1;

        // various end states
        private static final int RESOLVED = 0x10;
        private static final int FAILED = 0x11;
        private static final int CANCELLED = 0x12;

        private static final long serialVersionUID = -5044031197562766649L;

        private Object result;

        @Override
        protected int tryAcquireShared(int ignored) {
            return getState() >= RESOLVED ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int state) {
            setState(state);
            return true;
        }

        /**
         * Complete and provide a state, and a result to the syncer.
         *
         * @param state State to set.
         * @param result Result to provide.
         * @return {@code true} if the result was successfully provided, {@code false} otherwise.
         */
        private boolean complete(int state, Object result) {
            // short path: no result to provide.
            if (result == null) {
                if (!compareAndSetState(RUNNING, state))
                    return false;

                releaseShared(state);
                return true;
            }

            if (!compareAndSetState(RUNNING, RESULT))
                return false;

            this.result = result;
            setState(state);
            releaseShared(state);
            return true;
        }

        public boolean acquire(long nanos) throws InterruptedException {
            if (!tryAcquireNanos(-1, nanos))
                return false;

            return false;
        }

        public void acquire() throws InterruptedException {
            acquireSharedInterruptibly(-1);
        }

        /**
         * Fetch state without spinning, useful for just 'taking a look' without guaranteeing that a value has been set.
         */
        public int state() {
            return getState();
        }

        /**
         * Take the current state, and assert that a result has been set, if applicable.
         */
        public int poll() {
            // spin if the current state is changing.
            int spins = 0;
            int s;

            do {
                if (spins++ > MAX_SPINS) {
                    Thread.yield();
                    spins = 0;
                }
            } while ((s = getState()) == RESULT);

            return s;
        }

        public Object result() {
            return result;
        }
    }
}
