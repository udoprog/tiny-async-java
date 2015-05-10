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
    public boolean fail(Throwable cause) {
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
    public AsyncFuture<T> on(FutureDone<? super T> handle) {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> onAny(FutureDone<?> handle) {
        return on((FutureDone<T>) handle);
    }

    @Override
    public AsyncFuture<T> bind(AsyncFuture<?> other) {
        other.cancel();
        return this;
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

    @Override
    public AsyncFuture<T> on(FutureResolved<? super T> resolved) {
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFailed failed) {
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
    public <C> AsyncFuture<C> transform(LazyTransform<? super T, ? extends C> transform) {
        return async.cancelled();
    }

    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        return async.cancelled();
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, ? extends T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        final T result;

        try {
            result = transform.transform(null);
        } catch (Exception e) {
            return async.failed(e);
        }

        return async.resolved(result);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, ? extends T> transform) {
        return async.cancelled(this, transform);
    }
}