package eu.toolchain.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
public class FailedFutureTest {
    private static final Exception error = Mockito.mock(Exception.class);

    private AsyncFramework async;
    private AsyncCaller caller;
    private FutureDone<Integer> handle;
    private AsyncFuture<Boolean> failed;

    @Before
    public void before() {
        async = Mockito.mock(AsyncFramework.class);
        caller = Mockito.mock(AsyncCaller.class);
        handle = Mockito.mock(FutureDone.class);
        failed = new FailedFuture<Boolean>(async, caller, error);
    }

    @After
    public void after() throws Exception {
        Mockito.verify(caller, Mockito.never()).resolveFutureDone(handle, 42);
    }

    @Test
    public void testImmediatelyResolved() throws Exception {
        final AsyncFuture<Integer> c = new FailedFuture<Integer>(async, caller, error);
        c.on(handle);
    }

    @Test
    public void testTransform() throws Exception {
        final Transform<Boolean, Integer> transform = Mockito.mock(Transform.class);
        final AsyncFuture<Integer> secondFailed = Mockito.mock(AsyncFuture.class);
        Mockito.when(async.<Integer> failed(error)).thenReturn(secondFailed);

        failed.transform(transform).on(handle);

        Mockito.verify(async).failed(error);
        Mockito.verify(transform, Mockito.never()).transform(Mockito.anyBoolean());
    }

    @Test
    public void testDeferredTransform() throws Exception {
        final LazyTransform<Boolean, Integer> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Integer> secondFailed = Mockito.mock(AsyncFuture.class);
        Mockito.when(async.<Integer> failed(error)).thenReturn(secondFailed);

        failed.transform(transform).on(handle);

        Mockito.verify(async).failed(error);
        Mockito.verify(transform, Mockito.never()).transform(Mockito.anyBoolean());
    }
}