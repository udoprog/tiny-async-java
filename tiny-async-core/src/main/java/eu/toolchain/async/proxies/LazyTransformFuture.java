package eu.toolchain.async.proxies;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;

@RequiredArgsConstructor
public class LazyTransformFuture<S, T> implements FutureDone<S> {
    private final LazyTransform<S, T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable e) throws Exception {
        target.fail(e);
    }

    @Override
    public void resolved(S result) throws Exception {
        final AsyncFuture<T> t;

        try {
            t = transform.transform(result);
        } catch (Exception e) {
            failed(e);
            return;
        }

        t.on(new FutureDone<T>() {
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
    public void cancelled() throws Exception {
        target.cancel();
    }
}
