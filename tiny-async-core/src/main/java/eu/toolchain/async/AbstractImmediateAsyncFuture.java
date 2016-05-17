package eu.toolchain.async;

public abstract class AbstractImmediateAsyncFuture<T> extends DeprecatedCompatAsyncFuture<T> {
    protected AsyncFramework async;

    public AbstractImmediateAsyncFuture(AsyncFramework async) {
        this.async = async;
    }

    protected <C> AsyncFuture<C> transformResolved(
        final Transform<? super T, ? extends C> transform, final T result
    ) {
        final C transformed;

        try {
            transformed = transform.transform(result);
        } catch (final Exception e) {
            return async.failed(e);
        }

        return async.resolved(transformed);
    }

    protected <C> AsyncFuture<C> lazyTransformResolved(
        final LazyTransform<? super T, C> transform, final T result
    ) {
        try {
            return transform.transform(result);
        } catch (final Exception e) {
            return async.failed(e);
        }
    }

    protected AsyncFuture<T> transformFailed(
        final Transform<Throwable, ? extends T> transform, final Throwable cause
    ) {
        final T result;

        try {
            result = transform.transform(cause);
        } catch (final Exception e) {
            e.addSuppressed(cause);
            return async.failed(e);
        }

        return async.resolved(result);
    }

    protected AsyncFuture<T> lazyTransformFailed(
        final LazyTransform<Throwable, T> transform, final Throwable cause
    ) {
        try {
            return transform.transform(cause);
        } catch (final Exception e) {
            e.addSuppressed(cause);
            return async.failed(e);
        }
    }

    protected AsyncFuture<T> transformCancelled(final Transform<Void, ? extends T> transform) {
        final T transformed;

        try {
            transformed = transform.transform(null);
        } catch (final Exception e) {
            return async.failed(e);
        }

        return async.resolved(transformed);
    }

    protected AsyncFuture<T> lazyTransformCancelled(final LazyTransform<Void, T> transform) {
        try {
            return transform.transform(null);
        } catch (final Exception e) {
            return async.failed(e);
        }
    }
}
