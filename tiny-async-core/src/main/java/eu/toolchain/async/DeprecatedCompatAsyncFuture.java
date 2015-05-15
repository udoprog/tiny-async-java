package eu.toolchain.async;

/**
 * Acts as a compatibility layer for deprecated features on {@link AsyncFuture}.
 *
 * @author udoprog
 *
 * @param <T>
 */
public abstract class DeprecatedCompatAsyncFuture<T> implements AsyncFuture<T> {
    @Override
    public <R> AsyncFuture<R> transform(LazyTransform<? super T, R> transform) {
        return lazyTransform(transform);
    }

    @Override
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform) {
        return catchFailed(transform);
    }

    @Override
    public AsyncFuture<T> error(LazyTransform<Throwable, T> transform) {
        return lazyCatchFailed(transform);
    }

    @Override
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform) {
        return catchCancelled(transform);
    }

    @Override
    public AsyncFuture<T> cancelled(LazyTransform<Void, T> transform) {
        return lazyCatchCancelled(transform);
    }
}