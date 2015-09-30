package eu.toolchain.async.immediate;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

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

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
public class ImmediateCancelledAsyncFuture<T> extends AbstractImmediateAsyncFuture<T>implements AsyncFuture<T> {
    private final AsyncCaller caller;

    public ImmediateCancelledAsyncFuture(AsyncFramework async, AsyncCaller caller) {
        super(async);
        this.caller = caller;
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
    public AsyncFuture<T> onDone(FutureDone<? super T> handle) {
        caller.cancel(handle);
        return this;
    }

    @Override
    public AsyncFuture<T> bind(AsyncFuture<?> other) {
        other.cancel();
        return this;
    }

    @Override
    public AsyncFuture<T> onFinished(FutureFinished finishable) {
        caller.finish(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> onCancelled(FutureCancelled cancelled) {
        caller.cancel(cancelled);
        return this;
    }

    @Override
    public AsyncFuture<T> onResolved(FutureResolved<? super T> resolved) {
        return this;
    }

    @Override
    public AsyncFuture<T> onFailed(FutureFailed failed) {
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
    public <R> AsyncFuture<R> directTransform(Transform<? super T, ? extends R> transform) {
        return async.cancelled();
    }

    @Override
    public <R> AsyncFuture<R> lazyTransform(LazyTransform<? super T, R> transform) {
        return async.cancelled();
    }

    @Override
    public AsyncFuture<T> catchFailed(Transform<Throwable, ? extends T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> lazyCatchFailed(LazyTransform<Throwable, T> transform) {
        return this;
    }

    @Override
    public AsyncFuture<T> catchCancelled(Transform<Void, ? extends T> transform) {
        return transformCancelled(transform);
    }

    @Override
    public AsyncFuture<T> lazyCatchCancelled(LazyTransform<Void, T> transform) {
        return lazyTransformCancelled(transform);
    }
}