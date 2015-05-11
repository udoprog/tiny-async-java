package eu.toolchain.async;

public class FailedTransformHelperTest extends AbstractTransformHelperTest<Throwable> {
    private final Exception e = new Exception();

    @Override
    protected FutureDone<Object> setupDone(LazyTransform<Throwable, Object> transform, ResolvableFuture<Object> target) {
        return new FailedTransformHelper<Object>(transform, target);
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