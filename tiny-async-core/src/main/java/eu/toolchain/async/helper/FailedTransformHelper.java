package eu.toolchain.async.helper;

import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.Transform;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FailedTransformHelper<T> implements FutureDone<T> {
    private final Transform<Throwable, ? extends T> transform;
    private final ResolvableFuture<T> target;

    @Override
    public void failed(Throwable cause) throws Exception {
        final T value;

        try {
            value = transform.transform(cause);
        } catch (final Exception e) {
            target.fail(e);
            return;
        }

        target.resolve(value);
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
