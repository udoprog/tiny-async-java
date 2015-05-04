package eu.toolchain.async;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import lombok.RequiredArgsConstructor;

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

    private final Object $lock = new Object();

    private Sync sync = new Sync();

    private volatile ArrayList<CallbackEntry<T>> callbacks = new ArrayList<CallbackEntry<T>>();

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
        if (!this.sync.complete(Sync.RESOLVED, result))
            return false;

        final ArrayList<CallbackEntry<T>> entries = takeAndReset();

        for (final CallbackEntry<T> c : entries)
            c.resolved(result);

        return true;
    }

    @Override
    public boolean fail(Throwable cause) {
        if (!sync.complete(Sync.FAILED, cause))
            return false;

        final ArrayList<CallbackEntry<T>> entries = takeAndReset();

        for (final CallbackEntry<T> c : entries)
            c.failed(cause);

        return true;
    }

    @Override
    public boolean cancel() {
        return cancel(false);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!sync.complete(Sync.CANCELLED, null))
            return false;

        final ArrayList<CallbackEntry<T>> entries = takeAndReset();

        for (final CallbackEntry<T> c : entries)
            c.cancelled();

        return true;
    }

    /* listeners */

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> on(final FutureDone<? super T> done) {
        if (!sync.isDone() && add(new DoneEntry(done)))
            return this;

        final int state = this.sync.state();

        if (state == Sync.RUNNING)
            throw new IllegalStateException("result is not available");

        if (state == Sync.RESOLVED) {
            caller.resolveFutureDone(done, (T) this.sync.result());
            return this;
        }

        if (state == Sync.FAILED) {
            caller.failFutureDone(done, (Throwable) this.sync.result());
            return this;
        }

        if (state == Sync.CANCELLED) {
            caller.cancelFutureDone(done);
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
        if (!sync.isDone() && add(new CancelledEntry(cancelled)))
            return this;

        final int state = this.sync.state();

        if (state == Sync.RUNNING)
            throw new IllegalStateException("result is not available");

        if (state == Sync.CANCELLED)
            caller.runFutureCancelled(cancelled);

        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        if (!sync.isDone() && add(new FinishedEntry(finishable)))
            return this;

        caller.runFutureFinished(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureResolved<? super T> resolved) {
        if (!sync.isDone() && add(new ResolvedEntry(resolved)))
            return this;

        final int state = this.sync.state();

        if (state == Sync.RUNNING)
            throw new IllegalStateException("result is not available");

        if (state == Sync.RESOLVED)
            caller.runFutureResolved(resolved, (T) this.sync.result());

        return this;
    }

    /* check state */

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public boolean isCancelled() {
        return sync.state() == Sync.CANCELLED;
    }

    /* get result */

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (!isDone())
            sync.acquire();

        return checkState();
    }

    @Override
    public T getNow() throws ExecutionException {
        return checkState();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!sync.acquire(unit.toNanos(timeout)))
            throw new TimeoutException();

        return checkState();
    }

    @SuppressWarnings("unchecked")
    private T checkState() throws ExecutionException, CancellationException {
        final int state = sync.poll();

        if (state <= Sync.RESULT)
            throw new IllegalStateException("result is not ready");

        final Object result = this.sync.result();

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
     * Take and reset all callbacks.
     */
    private ArrayList<CallbackEntry<T>> takeAndReset() {
        final ArrayList<CallbackEntry<T>> entries;

        synchronized ($lock) {
            entries = callbacks;
            callbacks = null;
        }

        return entries;
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
    private boolean add(CallbackEntry<T> entry) {
        synchronized ($lock) {
            if (callbacks == null)
                return false;

            callbacks.add(entry);
        }

        return true;
    }

    private static interface CallbackEntry<T> {
        void resolved(T result);

        void failed(Throwable error);

        void cancelled();
    }

    @RequiredArgsConstructor
    private class DoneEntry implements CallbackEntry<T> {
        private final FutureDone<? super T> callback;

        @Override
        public void resolved(T result) {
            caller.resolveFutureDone(callback, result);
        }

        @Override
        public void failed(Throwable error) {
            caller.failFutureDone(callback, error);
        }

        @Override
        public void cancelled() {
            caller.cancelFutureDone(callback);
        }
    }

    @RequiredArgsConstructor
    private class ResolvedEntry implements CallbackEntry<T> {
        private final FutureResolved<? super T> callback;

        @Override
        public void resolved(T result) {
            caller.runFutureResolved(callback, result);
        }

        @Override
        public void failed(Throwable error) {
        }

        @Override
        public void cancelled() {
        }
    }

    @RequiredArgsConstructor
    private class FinishedEntry implements CallbackEntry<T> {
        private final FutureFinished callback;

        @Override
        public void resolved(T result) {
            caller.runFutureFinished(callback);
        }

        @Override
        public void failed(Throwable error) {
            caller.runFutureFinished(callback);
        }

        @Override
        public void cancelled() {
            caller.runFutureFinished(callback);
        }
    }

    @RequiredArgsConstructor
    private class CancelledEntry implements CallbackEntry<T> {
        private final FutureCancelled callback;

        @Override
        public void resolved(T result) {
        }

        @Override
        public void failed(Throwable error) {
        }

        @Override
        public void cancelled() {
            caller.runFutureCancelled(callback);
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
            if (!tryAcquireSharedNanos(-1, nanos))
                return false;

            return true;
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

        public boolean isDone() {
            return getState() > RESULT;
        }
    }
}
