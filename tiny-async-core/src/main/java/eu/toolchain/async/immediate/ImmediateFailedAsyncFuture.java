package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.Transform;
import eu.toolchain.async.TransformException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A callback which has already been resolved as 'failed'.
 *
 * @param <T>
 */
public class ImmediateFailedAsyncFuture<T> extends AbstractImmediateAsyncFuture<T>
    implements AsyncFuture<T> {
    private final AsyncCaller caller;
    private final Throwable cause;

    public ImmediateFailedAsyncFuture(AsyncFramework async, AsyncCaller caller, Throwable cause) {
        super(async);
        this.caller = caller;
        this.cause = cause;
    }

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
    public AsyncFuture<T> onDone(FutureDone<? super T> handle) {
        caller.fail(handle, cause);
        return this;
    }

    @Override
    public AsyncFuture<T> onFinished(FutureFinished finishable) {
        caller.finish(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> onCancelled(FutureCancelled cancelled) {
        return this;
    }

    @Override
    public AsyncFuture<T> onResolved(FutureResolved<? super T> resolved) {
        return this;
    }

    @Override
    public AsyncFuture<T> onFailed(FutureFailed failed) {
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
    public <R> AsyncFuture<R> directTransform(Transform<? super T, ? extends R> transform) {
        return async.failed(new TransformException(cause));
    }

    @Override
    public <R> AsyncFuture<R> lazyTransform(LazyTransform<? super T, R> transform) {
        return async.failed(new TransformException(cause));
    }

    @Override
    public AsyncFuture<T> catchFailed(Transform<Throwable, ? extends T> transform) {
        return transformFailed(transform, cause);
    }

    @Override
    public AsyncFuture<T> lazyCatchFailed(LazyTransform<Throwable, T> transform) {
        return lazyTransformFailed(transform, cause);
    }

    @Override
    public AsyncFuture<T> catchCancelled(Transform<Void, ? extends T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> lazyCatchCancelled(LazyTransform<Void, T> transform) {
        return this;
    }
}
