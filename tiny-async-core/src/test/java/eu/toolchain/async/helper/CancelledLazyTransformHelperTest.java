package eu.toolchain.async.helper;

import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;

public class CancelledLazyTransformHelperTest extends TransformHelperTestBase<Void> {
    @Override
    protected FutureDone<Object> setupDone(
        LazyTransform<Void, Object> transform, ResolvableFuture<Object> target
    ) {
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
