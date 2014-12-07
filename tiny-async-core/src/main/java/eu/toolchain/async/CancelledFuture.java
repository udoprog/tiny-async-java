package eu.toolchain.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
@RequiredArgsConstructor
public class CancelledFuture<T> implements AsyncFuture<T> {
    private final AsyncFramework async;
    private final AsyncCaller caller;

    /* transition */

    @Override
    public boolean fail(Throwable error) {
        return false;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /* register listeners */

    @Override
    public AsyncFuture<T> on(FutureDone<T> handle) {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> onAny(FutureDone<?> handle) {
        return on((FutureDone<T>) handle);
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        caller.runFutureFinished(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureCancelled cancelled) {
        caller.runFutureCancelled(cancelled);
        return this;
    }

    /* check state */

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /* get value */

    @Override
    public T get() {
        throw new CancellationException();
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        throw new CancellationException();
    }

    @Override
    public T getNow() {
        throw new CancellationException();
    }

    /* transform */

    @Override
    public <C> AsyncFuture<C> transform(LazyTransform<T, C> transform) {
        return async.cancelled(caller);
    }

    @Override
    public <C> AsyncFuture<C> transform(Transform<T, C> transform) {
        return async.cancelled(caller);
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, T> transform) {
        final T result;

        try {
            result = transform.transform(null);
        } catch(Exception e) {
            return async.failed(e, caller);
        }

        return async.resolved(result, caller);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, T> transform) {
        return async.cancelled(this, transform, caller);
    }
}