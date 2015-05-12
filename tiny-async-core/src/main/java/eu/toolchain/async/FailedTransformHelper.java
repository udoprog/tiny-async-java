package eu.toolchain.async;

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
        } catch (Exception e) {
            target.fail(new TransformException(e));
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
