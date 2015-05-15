package eu.toolchain.async;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
@RequiredArgsConstructor
public class ResolvedAsyncFuture<T> implements AsyncFuture<T> {
    private final AsyncFramework async;
    private final AsyncCaller caller;
    private final T result;

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
    public AsyncFuture<T> bind(AsyncFuture<?> other) {
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureDone<? super T> handle) {
        caller.resolve(handle, result);
        return this;
    }

    @Override
    public AsyncFuture<T> onAny(FutureDone<? super T> handle) {
        return on(handle);
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        caller.finish(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureCancelled cancelled) {
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureResolved<? super T> resolved) {
        caller.resolve(resolved, result);
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
        return true;
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /* get value */

    @Override
    public Throwable cause() {
        throw new IllegalStateException("future is not in a failed state");
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        return result;
    }

    @Override
    public T getNow() {
        return result;
    }

    /* transform */

    @Override
    public <R> AsyncFuture<R> transform(Transform<? super T, ? extends R> transform) {
        R value;

        try {
            value = transform.transform(result);
        } catch (Exception e) {
            return async.failed(e);
        }

        return async.resolved(value);
    }

    @Override
    public <R> AsyncFuture<R> transform(LazyTransform<? super T, R> transform) {
        try {
            return transform.transform(result);
        } catch (Exception e) {
            return async.failed(e);
        }
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
        return this;
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, T> transform) {
        return this;
    }
}