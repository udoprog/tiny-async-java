package eu.toolchain.async.helper;

import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;

public class FailedLazyTransformHelperTest extends TransformHelperTestBase<Throwable> {
    private final Exception e = new Exception();

    @Override
    protected FutureDone<Object> setupDone(
        LazyTransform<Throwable, Object> transform, ResolvableFuture<Object> target
    ) {
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
