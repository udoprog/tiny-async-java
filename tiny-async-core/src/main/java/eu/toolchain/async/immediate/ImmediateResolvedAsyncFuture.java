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

import java.util.concurrent.TimeUnit;

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
public class ImmediateResolvedAsyncFuture<T> extends AbstractImmediateAsyncFuture<T>
    implements AsyncFuture<T> {
    private final AsyncCaller caller;
    private final T result;

    public ImmediateResolvedAsyncFuture(AsyncFramework async, AsyncCaller caller, T result) {
        super(async);
        this.caller = caller;
        this.result = result;
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
        caller.resolve(handle, result);
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
        caller.resolve(resolved, result);
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
    public <R> AsyncFuture<R> directTransform(Transform<? super T, ? extends R> transform) {
        return transformResolved(transform, result);
    }

    @Override
    public <R> AsyncFuture<R> lazyTransform(LazyTransform<? super T, R> transform) {
        return lazyTransformResolved(transform, result);
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
        return this;
    }

    @Override
    public AsyncFuture<T> lazyCatchCancelled(LazyTransform<Void, T> transform) {
        return this;
    }
}
