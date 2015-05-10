package eu.toolchain.async;

import org.junit.Test;
import org.mockito.Mockito;

public class ConcurrentResolvableFutureTest extends AbstractTinyFutureTest<ConcurrentResolvableFuture<Object>> {
    private static final Object REFERENCE = new Object();

    @Override
    protected ConcurrentResolvableFuture<Object> newCallback() {
        return new ConcurrentResolvableFuture<Object>(getAsync(), getCaller());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInfiniteLoopProtection() {
        final AsyncFramework async = getAsync();
        final AsyncCaller caller = getCaller();

        // concurrent callback uses synchronized blocks and defers some actions
        // to outside of those blocks to avoid deadlocks.
        // This situation used to instigate a deadlock.
        final ConcurrentResolvableFuture<Object> c1 = new ConcurrentResolvableFuture<Object>(async, caller);
        final ConcurrentResolvableFuture<Object> c2 = new ConcurrentResolvableFuture<Object>(async, caller);

        final FutureDone<Object> d1 = Mockito.mock(FutureDone.class);
        final FutureDone<Object> d2 = Mockito.mock(FutureDone.class);

        c1.on(d1);
        c2.on(d2);

        c1.resolve(REFERENCE);
        c2.resolve(REFERENCE);

        Mockito.verify(caller).resolveFutureDone(d1, REFERENCE);
        Mockito.verify(caller).resolveFutureDone(d2, REFERENCE);
    }
}