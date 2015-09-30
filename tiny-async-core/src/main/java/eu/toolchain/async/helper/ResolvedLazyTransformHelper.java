package eu.toolchain.async.helper;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResolvedLazyTransformHelper<S, T> implements FutureDone<S> {
    private final LazyTransform<? super S, ? extends T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable e) throws Exception {
        target.fail(e);
    }

    @Override
    public void resolved(S result) throws Exception {
        final AsyncFuture<? extends T> t;

        try {
            t = transform.transform(result);
        } catch (Exception e) {
            failed(e);
            return;
        }

        t.onDone(new FutureDone<T>() {
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