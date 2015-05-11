package eu.toolchain.async;

public class CancelledLazyTransformHelperTest extends AbstractTransformHelperTest<Void> {
    @Override
    protected FutureDone<Object> setupDone(LazyTransform<Void, Object> transform, ResolvableFuture<Object> target) {
        return new CancelledLazyTransformHelper<Object>(transform, target);
    }

    @Override
    protected Void setupFrom() {
        return null;
    }

    @Override
    protected int setupCancelled() {
        return 1;
    }
}