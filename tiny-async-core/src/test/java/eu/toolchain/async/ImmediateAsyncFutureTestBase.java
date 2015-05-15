package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class ImmediateAsyncFutureTestBase {
    private final Throwable e = new Exception();
    private final Throwable cause = mock(Throwable.class);
    private final Object result = mock(Object.class);

    @Mock
    private AsyncFramework async;
    @Mock
    private AsyncCaller caller;
    @Mock
    private AsyncFuture<Object> other;
    @Mock
    private FutureDone<Object> done;
    @Mock
    private FutureFinished finished;
    @Mock
    private FutureFailed failed;
    @Mock
    private FutureCancelled cancelled;
    @Mock
    private FutureResolved<Object> resolved;

    private AbstractImmediateAsyncFuture<Object> future;

    private int cancelledTimes = 0;
    private int resolvedTimes = 0;
    private int failedTimes = 0;

    protected boolean setupResolved() {
        return false;
    }

    protected boolean setupCancelled() {
        return false;
    }

    protected boolean setupFailed() {
        return false;
    }

    protected abstract AbstractImmediateAsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result,
            Throwable cause);

    @Rule
    public ExpectedException except = ExpectedException.none();

    @Before
    public void setup() {
        future = spy(setupFuture(async, caller, result, cause));
        resolvedTimes = setupResolved() ? 1 : 0;
        cancelledTimes = setupCancelled() ? 1 : 0;
        failedTimes = setupFailed() ? 1 : 0;
    }

    @Test
    public void testFail() {
        assertFalse(future.fail(cause));
    }

    @Test
    public void testCancel() {
        assertFalse(future.cancel());
    }

    @Test
    public void testCancelWithParameter() {
        assertFalse(future.cancel(true));
        assertFalse(future.cancel(false));
    }

    @Test
    public void testOnFutureDone() throws Exception {
        future.on(done);
        verify(caller, times(cancelledTimes)).cancel(done);
        verify(caller, times(resolvedTimes)).resolve(done, result);
        verify(caller, times(failedTimes)).fail(done, cause);
    }

    @Test
    public void testOnAnyFuturedone() throws Exception {
        future.onAny(done);
        verify(caller, times(cancelledTimes)).cancel(done);
        verify(caller, times(resolvedTimes)).resolve(done, result);
        verify(caller, times(failedTimes)).fail(done, cause);
    }

    @Test
    public void testBind() throws Exception {
        future.bind(other);
        verify(other, times(cancelledTimes)).cancel();
    }

    @Test
    public void testOnFutureFinished() throws Exception {
        future.on(finished);
        verify(caller, times(1)).finish(finished);
    }

    @Test
    public void testOnFutureFailed() throws Exception {
        future.on(failed);
        verify(caller, times(failedTimes)).fail(failed, cause);
    }

    @Test
    public void testOnFutureCancelled() throws Exception {
        future.on(cancelled);
        verify(caller, times(cancelledTimes)).cancel(cancelled);
    }

    @Test
    public void testOnFutureResolved() throws Exception {
        future.on(resolved);
        verify(caller, times(resolvedTimes)).resolve(resolved, result);
    }

    @Test
    public void testIsDone() throws Exception {
        assertTrue(future.isDone());
    }

    @Test
    public void testIsResolved() throws Exception {
        assertEquals(resolvedTimes == 1, future.isResolved());
    }

    @Test
    public void testIsFailed() throws Exception {
        assertEquals(failedTimes == 1, future.isFailed());
    }

    @Test
    public void testIsCancelled() throws Exception {
        assertEquals(cancelledTimes == 1, future.isCancelled());
    }

    @Test
    public void testCause() throws Exception {
        if (cancelledTimes == 1 || resolvedTimes == 1)
            except.expect(IllegalStateException.class);

        assertNotNull(future.cause());
    }

    @Test
    public void testGet() throws Exception {
        if (cancelledTimes == 1)
            except.expect(CancellationException.class);

        if (failedTimes == 1)
            except.expect(ExecutionException.class);

        future.get();
    }

    @Test
    public void testGetWithTimeout() throws Exception {
        if (cancelledTimes == 1)
            except.expect(CancellationException.class);

        if (failedTimes == 1)
            except.expect(ExecutionException.class);

        future.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testGetNow() throws Exception {
        if (cancelledTimes == 1)
            except.expect(CancellationException.class);

        if (failedTimes == 1)
            except.expect(ExecutionException.class);

        future.getNow();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransform() throws Exception {
        final LazyTransform<Object, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = Mockito.mock(AsyncFuture.class);

        when(transform.transform(result)).thenReturn(f);

        final AsyncFuture<Object> returned = future.transform(transform);

        verify(async, times(cancelledTimes)).cancelled();
        verify(async, times(failedTimes)).failed(any(TransformException.class));
        verify(transform, times(resolvedTimes)).transform(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransformThrows() throws Exception {
        final LazyTransform<Object, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = Mockito.mock(AsyncFuture.class);

        when(transform.transform(result)).thenThrow(e);

        future.transform(transform);

        verify(async, times(cancelledTimes)).cancelled();
        verify(async, times(Math.max(resolvedTimes, failedTimes))).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransform() throws Exception {
        final Transform<Object, Object> transform = Mockito.mock(Transform.class);
        final Object transfomed = new Object();

        when(transform.transform(result)).thenReturn(transfomed);

        future.transform(transform);

        verify(async, times(cancelledTimes)).cancelled();
        verify(async, times(failedTimes)).failed(any(TransformException.class));
        verify(transform, times(resolvedTimes)).transform(result);
        verify(async, times(resolvedTimes)).resolved(transfomed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransformThrows() throws Exception {
        final Transform<Object, Object> transform = Mockito.mock(Transform.class);

        when(transform.transform(result)).thenThrow(e);

        future.transform(transform);

        verify(async, times(cancelledTimes)).cancelled();
        verify(async, times(Math.max(resolvedTimes, failedTimes))).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorTransform() throws Exception {
        final Transform<Throwable, Object> transform = Mockito.mock(Transform.class);
        final Object transfomed = new Object();

        when(transform.transform(cause)).thenReturn(transfomed);

        future.catchFailed(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, never()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorLazyTransformThrows() throws Exception {
        final LazyTransform<Throwable, Object> transform = Mockito.mock(LazyTransform.class);

        when(transform.transform(cause)).thenThrow(e);

        future.lazyCatchFailed(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, times(failedTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorLazyTransform() throws Exception {
        final LazyTransform<Throwable, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        when(transform.transform(cause)).thenReturn(f);

        final AsyncFuture<Object> transformed = future.lazyCatchFailed(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, never()).failed(any(TransformException.class));

        if (failedTimes == 1)
            assertEquals(f, transformed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorTransformThrows() throws Exception {
        final Transform<Throwable, Object> transform = Mockito.mock(Transform.class);

        when(transform.transform(cause)).thenThrow(e);

        future.catchFailed(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, times(failedTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledTransform() throws Exception {
        final Transform<Void, Object> transform = Mockito.mock(Transform.class);
        final Object transfomed = new Object();

        when(transform.transform(null)).thenReturn(transfomed);

        future.catchCancelled(transform);

        verify(transform, times(cancelledTimes)).transform(null);
        verify(async, never()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledTransformThrows() throws Exception {
        final Transform<Void, Object> transform = Mockito.mock(Transform.class);

        when(transform.transform(null)).thenThrow(e);

        future.catchCancelled(transform);

        verify(transform, times(cancelledTimes)).transform(null);
        verify(async, times(cancelledTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledLazyTransform() throws Exception {
        final LazyTransform<Void, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        when(transform.transform(null)).thenReturn(f);

        final AsyncFuture<Object> transformed = future.lazyCatchCancelled(transform);

        verify(transform, times(cancelledTimes)).transform(null);
        verify(async, never()).failed(any(TransformException.class));

        if (cancelledTimes == 1)
            assertEquals(f, transformed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledLazyTransformThrows() throws Exception {
        final LazyTransform<Void, Object> transform = Mockito.mock(LazyTransform.class);

        when(transform.transform(null)).thenThrow(e);

        future.lazyCatchCancelled(transform);

        verify(async, times(cancelledTimes)).failed(any(TransformException.class));
    }
}