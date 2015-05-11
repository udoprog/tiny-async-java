package eu.toolchain.async;

public class ResolvedTransformHelperTest extends AbstractTransformHelperTest<Object> {
    private final Object from = new Object();

    @Override
    protected FutureDone<Object> setupDone(LazyTransform<Object, Object> transform, ResolvableFuture<Object> target) {
        return new ResolvedTransformHelper<Object, Object>(transform, target);
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