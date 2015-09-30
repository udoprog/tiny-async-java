package eu.toolchain.async.helper;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.TransformException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FailedLazyTransformHelper<T> implements FutureDone<T> {
    private final LazyTransform<Throwable, ? extends T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable cause) throws Exception {
        final AsyncFuture<? extends T> future;

        try {
            future = transform.transform(cause);
        } catch (Exception e) {
            final TransformException inner = new TransformException(e);
            inner.addSuppressed(cause);
            target.fail(inner);
            return;
        }

        future.onDone(new FutureDone<T>() {
            @Override
            public void failed(Throwable e) throws Exception {
                target.fail(e);
            }

            @Override
            public void resolved(T result) throws Exception {
                target.resolve(result);
            }

            @Override
            public void cancelled() throws Exception {
                target.cancel();
            }
        });
    }

    @Override
    public void resolved(T result) throws Exception {
        target.resolve(result);
    }

    @Override
    public void cancelled() throws Exception {
        target.cancel();
    }
}