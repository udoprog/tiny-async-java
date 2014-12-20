package eu.toolchain.async.proxies;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.Transform;

@RequiredArgsConstructor
public class TransformErrorFutureProxy<T> implements AsyncFuture<T> {
    private final TinyAsync async;
    private final AsyncFuture<T> source;
    private final Transform<Throwable, ? extends T> transform;

    /* transition */

    @Override
    public boolean cancel() {
        return source.cancel();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return source.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean fail(Throwable error) {
        return source.fail(error);
    }

    /* register listeners */

    @Override
    public AsyncFuture<T> on(final FutureDone<? super T> handle) {
        source.on(new FutureDone<T>() {
            @Override
            public void failed(Throwable original) throws Exception {
                try {
                    handle.resolved(transform.transform(original));
                } catch (Exception inner) {
                    original.addSuppressed(original);
                    handle.failed(inner);
                }
            }

            @Override
            public void resolved(T result) throws Exception {
                handle.resolved(result);
            }

            @Override
            public void cancelled() throws Exception {
                handle.cancelled();
            }
        });

        return this;
    }

    @Override
    public AsyncFuture<T> onAny(FutureDone<?> handle) {
        source.onAny(handle);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureFinished finishable) {
        source.on(finishable);
        return this;
    }

    @Override
    public AsyncFuture<T> on(FutureCancelled cancelled) {
        source.on(cancelled);
        return this;
    }

    /* check state */

    @Override
    public boolean isDone() {
        return source.isDone();
    }

    @Override
    public boolean isCancelled() {
        return source.isCancelled();
    }

    /* get result */

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return source.get();
        } catch (ExecutionException e) {
            return getHandleException(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return source.get(timeout, unit);
        } catch (ExecutionException e) {
            return getHandleException(e);
        }
    }

    @Override
    public T getNow() throws ExecutionException {
        try {
            return source.getNow();
        } catch (ExecutionException e) {
            return getHandleException(e);
        }
    }

    private T getHandleException(ExecutionException original) throws ExecutionException {
        try {
            return transform.transform(original);
        } catch (Exception inner) {
            inner.addSuppressed(original);
            throw new ExecutionException(inner);
        }
    }

    /* transform */

    @Override
    public <C> AsyncFuture<C> transform(Transform<? super T, ? extends C> transform) {
        return async.transform(this, transform);
    }

    @Override
    public <C> AsyncFuture<C> transform(LazyTransform<? super T, ? extends C> transform) {
        return async.transform(this, transform);
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        return async.error(this, transform);
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, ? extends T> transform) {
        return async.error(this, transform);
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        return async.cancelled(this, transform);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, ? extends T> transform) {
        return async.cancelled(this, transform);
    }
}
