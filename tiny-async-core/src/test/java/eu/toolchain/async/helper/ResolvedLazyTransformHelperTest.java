package eu.toolchain.async.helper;

import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;

public class ResolvedLazyTransformHelperTest extends TransformHelperTestBase<Object> {
    private final Object from = new Object();

    @Override
    protected FutureDone<Object> setupDone(
        LazyTransform<Object, Object> transform, ResolvableFuture<Object> target
    ) {
        return new ResolvedLazyTransformHelper<Object, Object>(transform, target);
    }

    @Override
    protected Object setupFrom() {
        return from;
    }

    @Override
    protected int setupResolved() {
        return 1;
    }
}
