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
public class CancelledAsyncFuture<T> implements AsyncFuture<T> {
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
        caller.cancel(handle);
        return this;
    }

    @Override
    public AsyncFuture<T> onAny(FutureDone<? super T> handle) {
        return on(handle);
    }

    @Override
    public AsyncFuture<T> bind(AsyncFuture<?> other) {
        other.cancel();
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        caller.finish(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureCancelled cancelled) {
        caller.cancel(cancelled);
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
    public boolean isResolved() {
        return false;
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    /* get value */

    @Override
    public Throwable cause() {
        throw new IllegalStateException("future is not in a failed state");
    }

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
    public <R> AsyncFuture<R> transform(Transform<? super T, ? extends R> transform) {
        return async.cancelled();
    }

    @Override
    public <R> AsyncFuture<R> transform(LazyTransform<? super T, R> transform) {
        return async.cancelled();
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        final T result;

        try {
            result = transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(result);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, T> transform) {
        try {
            return transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }
}