package eu.toolchain.async;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeprecatedCompatAsyncFutureTest {
    @Mock
    private AsyncFuture<Object> future;
    @Mock
    private LazyTransform<Object, Object> lazyTransform;
    @Mock
    private Transform<Throwable, Object> catchFailed;
    @Mock
    private LazyTransform<Throwable, Object> lazyCatchFailed;
    @Mock
    private Transform<Void, Object> catchCancelled;
    @Mock
    private LazyTransform<Void, Object> lazyCatchCancelled;

    private DeprecatedCompatAsyncFuture<Object> compat;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        compat = mock(DeprecatedCompatAsyncFuture.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testLazyTransform() {
        doReturn(future).when(compat).lazyTransform(lazyTransform);
        assertEquals(future, compat.transform(lazyTransform));
        verify(compat).lazyTransform(lazyTransform);
    }

    @Test
    public void testCatchFailed() {
        doReturn(future).when(compat).catchFailed(catchFailed);
        assertEquals(future, compat.error(catchFailed));
        verify(compat).catchFailed(catchFailed);
    }

    @Test
    public void testLazyCatchFailed() {
        doReturn(future).when(compat).lazyCatchFailed(lazyCatchFailed);
        assertEquals(future, compat.error(lazyCatchFailed));
        verify(compat).lazyCatchFailed(lazyCatchFailed);
    }

    @Test
    public void testCatchCancelled() {
        doReturn(future).when(compat).catchCancelled(catchCancelled);
        assertEquals(future, compat.cancelled(catchCancelled));
        verify(compat).catchCancelled(catchCancelled);
    }

    @Test
    public void testLazyCatchCancelled() {
        doReturn(future).when(compat).lazyCatchCancelled(lazyCatchCancelled);
        assertEquals(future, compat.cancelled(lazyCatchCancelled));
        verify(compat).lazyCatchCancelled(lazyCatchCancelled);
    }
}
