package eu.toolchain.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

/**
 * A callback which has already been resolved as 'failed'.
 *
 * @param <T>
 */
@RequiredArgsConstructor
public class FailedAsyncFuture<T> implements AsyncFuture<T> {
    private final AsyncFramework async;
    private final AsyncCaller caller;
    private final Throwable cause;

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
        caller.failFutureDone(handle, cause);
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
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFailed failed) {
        caller.runFutureFailed(failed, cause);
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
    public T get() throws ExecutionException {
        throw new ExecutionException(cause);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        throw new ExecutionException(cause);
    }

    @Override
    public T getNow() throws ExecutionException {
        throw new ExecutionException(cause);
    }

    /* transform */

    @Override
    public <C> AsyncFuture<C> transform(LazyTransform<? super T, ? extends C> transform) {
        return async.failed(cause);
    }

    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        return async.failed(cause);
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        final T result;

        try {
            result = transform.transform(cause);
        } catch (Exception e) {
            final TransformException inner = new TransformException(e);
            inner.addSuppressed(cause);
            return async.failed(inner);
        }

        return async.resolved(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, ? extends T> transform) {
        try {
            return (AsyncFuture<T>) transform.transform(cause);
        } catch (Exception e) {
            final TransformException inner = new TransformException(e);
            inner.addSuppressed(cause);
            return async.failed(e);
        }
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