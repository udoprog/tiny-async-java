package eu.toolchain.async;

public abstract class AbstractImmediateAsyncFuture<T> extends DeprecatedCompatAsyncFuture<T> {
    protected AsyncFramework async;

    public AbstractImmediateAsyncFuture(AsyncFramework async) {
        this.async = async;
    }

    protected <C> AsyncFuture<C> transformResolved(final Transform<? super T, ? extends C> transform, final T result) {
        final C transformed;

        try {
            transformed = transform.transform(result);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(transformed);
    }

    protected <C> AsyncFuture<C> lazyTransformResolved(
            final LazyTransform<? super T, C> transform, final T result) {
        try {
            return transform.transform(result);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }

    protected AsyncFuture<T> transformFailed(final Transform<Throwable, ? extends T> transform, final Throwable cause) {
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

    protected AsyncFuture<T> lazyTransformFailed(final LazyTransform<Throwable, T> transform,
            final Throwable cause) {
        try {
            return transform.transform(cause);
        } catch (Exception e) {
            final TransformException inner = new TransformException(e);
            inner.addSuppressed(cause);
            return async.failed(inner);
        }
    }

    protected AsyncFuture<T> transformCancelled(final Transform<Void, ? extends T> transform) {
        final T transformed;

        try {
            transformed = transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }

        return async.resolved(transformed);
    }

    protected AsyncFuture<T> lazyTransformCancelled(final LazyTransform<Void, T> transform) {
        try {
            return transform.transform(null);
        } catch (Exception e) {
            return async.failed(new TransformException(e));
        }
    }
}