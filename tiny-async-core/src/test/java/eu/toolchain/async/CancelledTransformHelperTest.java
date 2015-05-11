package eu.toolchain.async;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class CancelledTransformHelperTest {
    private LazyTransform<Void, Object> transform;
    private ResolvableFuture<Object> target;
    private CancelledTransformHelper<Object> helper;

    private final Exception e = new Exception();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        transform = mock(LazyTransform.class);
        target = mock(ResolvableFuture.class);
        helper = new CancelledTransformHelper<Object>(transform, target);
    }

    @Test
    public void testFailed() throws Exception {
        helper.failed(e);
        verify(target).fail(e);
    }

    @Test
    public void testCancelledThrows() throws Exception {
        doThrow(e).when(transform).transform(null);
        helper.cancelled();
        verify(target).fail(e);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelled() throws Exception {
        final AsyncFuture<Object> f = mock(AsyncFuture.class);
        doReturn(f).when(transform).transform(null);
        helper.cancelled();
        verify(f).on(any(FutureDone.class));
    }
}