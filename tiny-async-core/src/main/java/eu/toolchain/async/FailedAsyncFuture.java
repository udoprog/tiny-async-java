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
        caller.fail(handle, cause);
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
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFailed failed) {
        caller.fail(failed, cause);
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
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /* get value */

    @Override
    public Throwable cause() {
        return cause;
    }

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
    public <R> AsyncFuture<R> transform(Transform<? super T, ? extends R> transform) {
        return async.failed(new TransformException(cause));
    }

    @Override
    public <R> AsyncFuture<R> transform(LazyTransform<? super T, R> transform) {
        return async.failed(new TransformException(cause));
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

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, T> transform) {
        try {
            return transform.transform(cause);
        } catch (Exception e) {
            final TransformException inner = new TransformException(e);
            inner.addSuppressed(cause);
            return async.failed(inner);
        }
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