package eu.toolchain.async;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
@RequiredArgsConstructor
public class ResolvedFuture<T> implements AsyncFuture<T> {
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
        caller.resolveFutureDone(handle, result);
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
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureResolved<? super T> resolved) {
        caller.runFutureResolved(resolved, result);
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

    @SuppressWarnings("unchecked")
    @Override
    public <C> AsyncFuture<C> transform(LazyTransform<? super T, ? extends C> transform) {
        try {
            return (AsyncFuture<C>) transform.transform(result);
        } catch (Exception e) {
            return async.failed(e);
        }
    }

    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        C value;

        try {
            value = transform.transform(result);
        } catch (Exception e) {
            return async.failed(e);
        }

        return async.resolved(value);
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
        return this;
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, ? extends T> transform) {
        return this;
    }
}