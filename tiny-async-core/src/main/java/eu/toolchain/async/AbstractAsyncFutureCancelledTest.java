package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAsyncFutureCancelledTest {
    private static final Throwable cause = new Exception();

    private AbstractImmediateAsyncFuture<To> base;

    @Mock
    private AsyncFramework async;
    @Mock
    private Transform<Void, To> transform;
    @Mock
    private LazyTransform<Void, To> lazyTransform;
    @Mock
    private To to;
    @Mock
    private AsyncFuture<To> resolved;
    @Mock
    private AsyncFuture<To> failed;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        base = mock(AbstractImmediateAsyncFuture.class, Mockito.CALLS_REAL_METHODS);
        base.async = async;
    }

    @Test
    public void testTransformResolved() throws Exception {
        doReturn(to).when(transform).transform(null);
        doReturn(resolved).when(async).resolved(to);
        doReturn(failed).when(async).failed(any(TransformException.class));

        assertEquals(resolved, base.transformCancelled(transform));

        final InOrder order = inOrder(transform, async);
        order.verify(transform).transform(null);
        order.verify(async).resolved(to);
        order.verify(async, never()).failed(any(TransformException.class));
    }

    @Test
    public void testTransformResolvedThrows() throws Exception {
        doThrow(cause).when(transform).transform(null);
        doReturn(resolved).when(async).resolved(to);
        doReturn(failed).when(async).failed(any(TransformException.class));

        assertEquals(failed, base.transformCancelled(transform));

        final InOrder order = inOrder(transform, async);
        order.verify(transform).transform(null);
        order.verify(async, never()).resolved(to);
        order.verify(async).failed(any(TransformException.class));
    }

    @Test
    public void testTransformLazyResolved() throws Exception {
        doReturn(resolved).when(lazyTransform).transform(null);
        doReturn(failed).when(async).failed(any(TransformException.class));

        assertEquals(resolved, base.lazyTransformCancelled(lazyTransform));

        final InOrder order = inOrder(lazyTransform, async);
        order.verify(lazyTransform).transform(null);
        order.verify(async, never()).failed(any(TransformException.class));
    }

    @Test
    public void testTransformLazyResolvedThrows() throws Exception {
        doThrow(cause).when(lazyTransform).transform(null);
        doReturn(failed).when(async).failed(any(TransformException.class));

        assertEquals(failed, base.lazyTransformCancelled(lazyTransform));

        final InOrder order = inOrder(lazyTransform, async);
        order.verify(lazyTransform).transform(null);
        order.verify(async).failed(any(TransformException.class));
    }

    public interface To {
    }
}