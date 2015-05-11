package eu.toolchain.async;

public class FailedLazyTransformHelperTest extends AbstractTransformHelperTest<Throwable> {
    private final Exception e = new Exception();

    @Override
    protected FutureDone<Object> setupDone(LazyTransform<Throwable, Object> transform, ResolvableFuture<Object> target) {
        return new FailedLazyTransformHelper<Object>(transform, target);
    }

    @Override
    protected Throwable setupFrom() {
        return e;
    }

    @Override
    protected Throwable setupError() {
        return e;
    }

    @Override
    protected int setupFailed() {
        return 1;
    }
}