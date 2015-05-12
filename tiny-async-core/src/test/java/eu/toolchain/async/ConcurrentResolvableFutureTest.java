package eu.toolchain.async;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class ConcurrentResolvableFutureTest {
    private static final Object result = new Object();

    private AsyncFramework async;
    private AsyncCaller caller;
    private ConcurrentResolvableFuture<Object> future;

    @Before
    public void setup() {
        async = mock(AsyncFramework.class);
        caller = mock(AsyncCaller.class);
        future = new ConcurrentResolvableFuture<>(async, caller);
    }

    @Test
    public void testInfiniteLoopProtection() {
    }
}