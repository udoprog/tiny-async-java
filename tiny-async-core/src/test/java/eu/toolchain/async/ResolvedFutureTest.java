package eu.toolchain.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
public class ResolvedFutureTest {
    private final Object result = new Object();
    private final Object result2 = new Object();

    private AsyncFramework async;
    private AsyncCaller caller;
    private FutureDone<Object> handle;
    private AsyncFuture<Object> future;

    @Before
    public void before() {
        async = Mockito.mock(AsyncFramework.class);
        caller = Mockito.mock(AsyncCaller.class);
        handle = Mockito.mock(FutureDone.class);
        future = new ResolvedFuture<>(async, caller, result);
    }

    @After
    public void after() throws Exception {
        Mockito.verify(handle, Mockito.never()).failed(Mockito.any(Exception.class));
    }

    @Test
    public void testImmediatelyResolved() throws Exception {
        final AsyncFuture<Object> c = new ResolvedFuture<>(async, caller, result);
        c.on(handle);
    }

    @Test
    public void testTransform() throws Exception {
        final Transform<Object, Object> transform = Mockito.mock(Transform.class);
        Mockito.when(transform.transform(result)).thenReturn(result2);
        final AsyncFuture<Object> second = Mockito.mock(AsyncFuture.class);
        Mockito.when(async.resolved(result2)).thenReturn(second);

        future.transform(transform).on(handle);

        Mockito.verify(transform).transform(result);
        Mockito.verify(async).resolved(result2);
    }

    @Test
    public void testLazyTransform() throws Exception {
        final LazyTransform<Object, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> second = Mockito.mock(AsyncFuture.class);
        Mockito.when(transform.transform(result)).thenReturn(second);

        future.transform(transform).on(handle);

        Mockito.verify(transform).transform(result);
    }
}