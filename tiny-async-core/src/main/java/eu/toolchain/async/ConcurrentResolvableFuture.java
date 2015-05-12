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
public class ConcurrentResolvableFuture<T> implements ResolvableFuture<T> {
    // waiting for value.
    public static final int RUNNING = 0x0;
    public static final int RESULT_UPDATING = 0x1;

    /* valid end state, are required to be greater than RESULT_UPDATING, otherwise {@link isStateReady(int)} will fail. */
    public static final int RESOLVED = 0x10;
    public static final int FAILED = 0x11;
    public static final int CANCELLED = 0x12;

    /**
     * The maximum number of spins allowed when busy-waiting.
     */
    private static final int MAX_SPINS = 10;

    private final Object $lock = new Object();

    private final S sync;

    private volatile ArrayList<CB<T>> callbacks = new ArrayList<CB<T>>();

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
    public ConcurrentResolvableFuture(final AsyncFramework async, final AsyncCaller caller) {
        this(async, caller, new Sync());
    }

    protected ConcurrentResolvableFuture(final AsyncFramework async, final AsyncCaller caller, final S sync) {
        this.async = async;
        this.caller = caller;
        this.sync = sync;
    }

    /* transition */

    @Override
    public boolean resolve(T result) {
        if (!this.sync.complete(RESOLVED, result))
            return false;

        final ArrayList<CB<T>> entries = takeAndClear();

        for (final CB<T> c : entries)
            c.resolved(result);

        return true;
    }

    @Override
    public boolean fail(Throwable cause) {
        if (!sync.complete(FAILED, cause))
            return false;

        final ArrayList<CB<T>> entries = takeAndClear();

        for (final CB<T> c : entries)
            c.failed(cause);

        return true;
    }

    @Override
    public boolean cancel() {
        return cancel(false);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!sync.complete(CANCELLED, null))
            return false;

        final ArrayList<CB<T>> entries = takeAndClear();

        for (final CB<T> c : entries)
            c.cancelled();

        return true;
    }

    /* listeners */

    @Override
    public AsyncFuture<T> bind(AsyncFuture<?> other) {
        int state = this.sync.state();

        if (!isStateReady(state)) {
            if (add(new AsyncFutureCB(other)))
                return this;

            state = sync.poll();
        }

        if (state == CANCELLED)
            other.cancel();

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> on(final FutureDone<? super T> done) {
        int state = this.sync.state();

        if (!isStateReady(state)) {
            if (add(new DoneCB(done)))
                return this;

            state = sync.poll();
        }

        if (state == RESOLVED) {
            caller.resolveFutureDone(done, (T) this.sync.result(state));
            return this;
        }

        if (state == FAILED) {
            caller.failFutureDone(done, (Throwable) this.sync.result(state));
            return this;
        }

        if (state == CANCELLED) {
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
        int state = this.sync.state();

        if (!isStateReady(state)) {
            if (add(new CancelledCB(cancelled)))
                return this;

            state = sync.poll();
        }

        if (state == CANCELLED)
            caller.runFutureCancelled(cancelled);

        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        int state = sync.state();

        if (!isStateReady(state) && add(new FinishedCB(finishable)))
            return this;

        caller.runFutureFinished(finishable);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> on(FutureResolved<? super T> resolved) {
        int state = this.sync.state();

        if (!isStateReady(state)) {
            if (add(new ResolvedCB(resolved)))
                return this;

            state = sync.poll();
        }

        if (state == RESOLVED)
            caller.runFutureResolved(resolved, (T) this.sync.result(state));

        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFailed failed) {
        int state = this.sync.state();

        if (!isStateReady(state)) {
            if (add(new FailedCB(failed)))
                return this;

            state = sync.poll();
        }

        if (state == FAILED)
            caller.runFutureFailed(failed, (Throwable) this.sync.result(state));

        return this;
    }

    /* check state */

    @Override
    public boolean isDone() {
        return isStateReady(sync.state());
    }

    @Override
    public boolean isCancelled() {
        return sync.state() == CANCELLED;
    }

    /* get result */

    @Override
    public T get() throws InterruptedException, ExecutionException {
        final int state = sync.state();

        if (isStateReady(state))
            return checkState(state);

        sync.acquire();
        return checkState(sync.state());
    }

    @Override
    public T getNow() throws ExecutionException {
        final int state = sync.state();

        if (!isStateReady(state))
            throw new IllegalStateException("sync state is not ready");

        return checkState(state);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final int state = sync.state();

        if (isStateReady(state))
            return checkState(state);

        if (!sync.acquire(unit.toNanos(timeout)))
            throw new TimeoutException();

        return checkState(sync.poll());
    }

    @SuppressWarnings("unchecked")
    private T checkState(int state) throws ExecutionException, CancellationException {
        switch (state) {
        case FAILED:
            throw new ExecutionException((Throwable) this.sync.result(state));
        case RESOLVED:
            return (T) this.sync.result(state);
        case CANCELLED:
            throw new CancellationException();
        default:
            throw new IllegalStateException("illegal state: " + state);
        }
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

        if (state == FAILED) {
            final Throwable e = (Throwable) sync.result(state);
            return async.failed(e);
        }

        final T result = (T) sync.result(state);
        final C transformed;

        try {
            transformed = transform.transform(result);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(transformed);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> AsyncFuture<C> transform(final LazyTransform<? super T, ? extends C> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.transform(this, transform);

        // shortcut

        if (state == CANCELLED)
            return async.cancelled();

        if (state == FAILED) {
            final Throwable e = (Throwable) sync.result(state);
            return async.failed(e);
        }

        final T result = (T) sync.result(state);

        try {
            return (AsyncFuture<C>) transform.transform(result);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.error(this, transform);

        // shortcut

        if (state == CANCELLED)
            return async.cancelled();

        if (state == RESOLVED)
            return async.resolved((T) sync.result(state));

        final Throwable cause = (Throwable) sync.result(state);

        final T transformed;

        try {
            transformed = transform.transform(cause);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(transformed);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.error(this, transform);

        // shortcut

        if (state == CANCELLED)
            return async.cancelled();

        if (state == RESOLVED)
            return async.resolved((T) sync.result(state));

        final Throwable cause = (Throwable) sync.result(state);

        try {
            return (AsyncFuture<T>) transform.transform(cause);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.cancelled(this, transform);

        // shortcut

        if (state == FAILED)
            return this;

        if (state == RESOLVED)
            return this;

        final T transformed;

        try {
            transformed = transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(transformed);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, ? extends T> transform) {
        final int state = sync.state();

        if (!isStateReady(state))
            return async.cancelled(this, transform);

        // shortcut

        if (state == FAILED)
            return this;

        if (state == RESOLVED)
            return this;

        try {
            return (AsyncFuture<T>) transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }

    /**
     * Take and reset all callbacks.
     */
    private ArrayList<CB<T>> takeAndClear() {
        final ArrayList<CB<T>> entries;

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
    private boolean add(CB<T> entry) {
        synchronized ($lock) {
            if (callbacks == null)
                return false;

            callbacks.add(entry);
        }

        return true;
    }

    private static interface CB<T> {
        void resolved(T result);

        void failed(Throwable cause);

        void cancelled();
    }

    @RequiredArgsConstructor
    private class AsyncFutureCB implements CB<T> {
        private final AsyncFuture<?> other;

        @Override
        public void resolved(T result) {
        }

        @Override
        public void failed(Throwable cause) {
        }

        @Override
        public void cancelled() {
            other.cancel();
        }
    }

    @RequiredArgsConstructor
    private class DoneCB implements CB<T> {
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
    private class FailedCB implements CB<T> {
        private final FutureFailed callback;

        @Override
        public void resolved(T result) {
        }

        @Override
        public void failed(Throwable cause) {
            caller.runFutureFailed(callback, cause);
        }

        @Override
        public void cancelled() {
        }
    }

    @RequiredArgsConstructor
    private class ResolvedCB implements CB<T> {
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
    private class FinishedCB implements CB<T> {
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
    private class CancelledCB implements CB<T> {
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

    public static boolean isStateReady(int state) {
        return state > RESULT_UPDATING;
    }

    public static boolean isStateCancelled(int state) {
        return state == CANCELLED;
    }

    protected interface S {
        /**
         * Get the current state.
         *
         * This must be an atomic operation.
         */
        public int state();

        /**
         * Poll for the current state, this is guaranteed to never return {@code RESULT_UPDATING}.
         */
        public int poll();

        /**
         * Acquire the shared lock on this synchronizer.
         *
         * Will block until the shared lock has been acquired.
         */
        public void acquire() throws InterruptedException;

        /**
         * Acquire the shared lock on this synchronizer.
         *
         * Will block for {@code nanos} nanoseconds until the shared lock has been acquired.
         *
         * @return {@code true} if the lock was acquired, {@code false} otherwise.
         */
        public boolean acquire(long nanos) throws InterruptedException;

        /**
         * Move the synchronizer state to the given end state.
         *
         * @param state The end state to move the synchronizer into.
         * @param result The result to associate with the end state, if not {@code null} will cause the synchronizer to
         *            go into an intermediate {@code RESULT_UPDATING} state that has to be accounted for. Use
         *            {@link #poll()} to avoid this.
         * @return {@code true} if the given transition is valid and happened, {@code false} otherwise}.
         */
        public boolean complete(int state, Object result);

        /**
         * Fetch the result of the synchronizer.
         *
         * @param state The state that you expect the synchronizer to currently be in.
         * @return The associated result, or {@code null} if there are none (e.g. state is {@code CANCELLED}).
         */
        public Object result(int state);
    }

    private static class Sync extends AbstractQueuedSynchronizer implements S {
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
        @Override
        public boolean complete(int state, Object result) {
            // short path: no result to provide.
            if (result == null) {
                if (!compareAndSetState(RUNNING, state))
                    return false;

                releaseShared(state);
                return true;
            }

            if (!compareAndSetState(RUNNING, RESULT_UPDATING))
                return false;

            this.result = result;
            releaseShared(state);
            return true;
        }

        @Override
        public boolean acquire(long nanos) throws InterruptedException {
            if (!tryAcquireSharedNanos(-1, nanos))
                return false;

            return true;
        }

        @Override
        public void acquire() throws InterruptedException {
            acquireSharedInterruptibly(-1);
        }

        /**
         * Fetch state without spinning, useful for just 'taking a look' without guaranteeing that a value has been set.
         */
        @Override
        public int state() {
            return getState();
        }

        /**
         * Take the current state, and assert that a result has been set, if applicable.
         */
        @Override
        public int poll() {
            // spin if the current state is changing.
            int s;
            int spins = 0;

            while ((s = getState()) == RESULT_UPDATING) {
                if (spins++ > MAX_SPINS) {
                    Thread.yield();
                    spins = 0;
                }
            }

            return s;
        }

        @Override
        public Object result(int state) {
            if (!isStateReady(state))
                throw new IllegalStateException("given state is not a valid end-state");

            return result;
        }
    }
}
