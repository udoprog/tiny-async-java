package eu.toolchain.async;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAsyncFutureFailedTest {
    private AbstractImmediateAsyncFuture<To> base;

    @Mock
    private AsyncFramework async;
    @Mock
    private Transform<Throwable, To> transform;
    @Mock
    private LazyTransform<Throwable, To> lazyTransform;
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
        final Throwable cause = new Exception();

        doReturn(to).when(transform).transform(cause);
        doReturn(resolved).when(async).resolved(to);
        doReturn(failed).when(async).failed(any(Exception.class));

        assertEquals(resolved, base.transformFailed(transform, cause));

        final InOrder order = inOrder(transform, async);
        order.verify(transform).transform(cause);
        order.verify(async).resolved(to);
        order.verify(async, never()).failed(any(Exception.class));
    }

    @Test
    public void testTransformResolvedThrows() throws Exception {
        final Throwable thrown = new Exception();
        final Throwable cause = new Exception();

        doThrow(thrown).when(transform).transform(cause);
        doReturn(resolved).when(async).resolved(to);
        doReturn(failed).when(async).failed(any(Exception.class));

        assertEquals(failed, base.transformFailed(transform, cause));

        final InOrder order = inOrder(transform, async);
        order.verify(transform).transform(cause);
        order.verify(async, never()).resolved(to);
        order.verify(async).failed(any(Exception.class));
    }

    @Test
    public void testTransformLazyResolved() throws Exception {
        final Throwable cause = new Exception();

        doReturn(resolved).when(lazyTransform).transform(cause);
        doReturn(failed).when(async).failed(any(Exception.class));

        assertEquals(resolved, base.lazyTransformFailed(lazyTransform, cause));

        final InOrder order = inOrder(lazyTransform, async);
        order.verify(lazyTransform).transform(cause);
        order.verify(async, never()).failed(any(Exception.class));
    }

    @Test
    public void testTransformLazyResolvedThrows() throws Exception {
        final Throwable thrown = new Exception();
        final Throwable cause = new Exception();

        doThrow(thrown).when(lazyTransform).transform(cause);
        doReturn(failed).when(async).failed(any(Exception.class));

        assertEquals(failed, base.lazyTransformFailed(lazyTransform, cause));

        final InOrder order = inOrder(lazyTransform, async);
        order.verify(lazyTransform).transform(cause);
        order.verify(async).failed(any(Exception.class));
    }

    public interface To {
    }
}
