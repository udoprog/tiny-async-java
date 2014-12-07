package eu.toolchain.async;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.ConcurrentFuture;

public class ConcurrentFutureTest extends AbstractFutureTest<ConcurrentFuture<Object>> {
    private static final Object REFERENCE = new Object();

    private AsyncCaller caller;
    private AsyncFramework async;

    @Before
    public void setup() {
        caller = Mockito.mock(AsyncCaller.class);
        async = Mockito.mock(AsyncFramework.class);
    }

    @Override
    protected ConcurrentFuture<Object> newCallback(AsyncCaller caller) {
        return new ConcurrentFuture<Object>(async, caller);
    }

    @Test
    public void testInfiniteLoopProtection() {
        // concurrent callback uses synchronized blocks and defers some actions
        // to outside of those blocks to avoid deadlocks.
        // This situation used to instigate a deadlock.
        final ConcurrentFuture<Object> c1 = new ConcurrentFuture<Object>(async, caller);
        final ConcurrentFuture<Object> c2 = new ConcurrentFuture<Object>(async, caller);
        c1.on(c2);
        c2.on(c1);

        c1.resolve(REFERENCE);
        Mockito.verify(caller).resolveFutureDone(c2, REFERENCE);

        c2.resolve(REFERENCE);
        Mockito.verify(caller).resolveFutureDone(c1, REFERENCE);
    }
}
