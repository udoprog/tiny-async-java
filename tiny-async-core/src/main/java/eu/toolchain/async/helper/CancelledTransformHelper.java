package eu.toolchain.async.helper;

import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.Transform;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelledTransformHelper<T> implements FutureDone<T> {
    private final Transform<Void, ? extends T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable cause) throws Exception {
        target.fail(cause);
    }

    @Override
    public void resolved(T result) throws Exception {
        target.resolve(result);
    }

    @Override
    public void cancelled() throws Exception {
        final T value;

        try {
            value = transform.transform(null);
        } catch (Exception e) {
            target.fail(e);
            return;
        }

        target.resolve(value);
    }
}
