package eu.toolchain.async.proxies;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;

@RequiredArgsConstructor
public class LazyTransformErrorFuture<T> implements FutureDone<T> {
    private final LazyTransform<Throwable, T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable cause) throws Exception {
        final AsyncFuture<T> future;

        try {
            future = transform.transform(cause);
        } catch(Exception e) {
            e.addSuppressed(cause);
            target.fail(e);
            return;
        }

        future.on(new FutureDone<T>() {
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