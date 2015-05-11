package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.mockito.Mockito;

public abstract class AbstractImmediateAsyncFuture {
    private AsyncFuture<Object> future;

    private AsyncFramework async;
    private AsyncCaller caller;
    private AsyncFuture<Object> other;
    private FutureDone<Object> done;
    private FutureFinished finished;
    private FutureFailed failed;
    private FutureCancelled cancelled;
    private FutureResolved<Object> resolved;

    private final Throwable cause = mock(Throwable.class);
    private Throwable e;
    private final Object result = mock(Object.class);

    private int cancelledTimes = 0;
    private int resolvedTimes = 0;
    private int failedTimes = 0;

    protected int setupResolved() {
        return 0;
    }

    protected int setupCancelled() {
        return 0;
    }

    protected int setupFailed() {
        return 0;
    }

    protected abstract AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result,
            Throwable cause);

    @Rule
    public ExpectedException except = ExpectedException.none();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        e = new Exception();

        async = mock(AsyncFramework.class);
        caller = mock(AsyncCaller.class);
        other = mock(AsyncFuture.class);
        done = mock(FutureDone.class);
        finished = mock(FutureFinished.class);
        failed = mock(FutureFailed.class);
        cancelled = mock(FutureCancelled.class);
        resolved = mock(FutureResolved.class);

        future = setupFuture(async, caller, result, cause);
        resolvedTimes = setupResolved();
        cancelledTimes = setupCancelled();
        failedTimes = setupFailed();
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
        verify(caller, times(cancelledTimes)).cancelFutureDone(done);
        verify(caller, times(resolvedTimes)).resolveFutureDone(done, result);
        verify(caller, times(failedTimes)).failFutureDone(done, cause);
    }

    @Test
    public void testOnAnyFuturedone() throws Exception {
        future.onAny(done);
        verify(caller, times(cancelledTimes)).cancelFutureDone(done);
        verify(caller, times(resolvedTimes)).resolveFutureDone(done, result);
        verify(caller, times(failedTimes)).failFutureDone(done, cause);
    }

    @Test
    public void testBind() throws Exception {
        future.bind(other);
        verify(other, times(cancelledTimes)).cancel();
    }

    @Test
    public void testOnFutureFinished() throws Exception {
        future.on(finished);
        verify(caller, times(1)).runFutureFinished(finished);
    }

    @Test
    public void testOnFutureFailed() throws Exception {
        future.on(failed);
        verify(caller, times(failedTimes)).runFutureFailed(failed, cause);
    }

    @Test
    public void testOnFutureCancelled() throws Exception {
        future.on(cancelled);
        verify(caller, times(cancelledTimes)).runFutureCancelled(cancelled);
    }

    @Test
    public void testOnFutureResolved() throws Exception {
        future.on(resolved);
        verify(caller, times(resolvedTimes)).runFutureResolved(resolved, result);
    }

    @Test
    public void testIsDone() throws Exception {
        assertTrue(future.isDone());
    }

    @Test
    public void testIsCancelled() throws Exception {
        assertEquals(cancelledTimes == 1, future.isCancelled());
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

        future.error(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, never()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorLazyTransformThrows() throws Exception {
        final LazyTransform<Throwable, Object> transform = Mockito.mock(LazyTransform.class);

        when(transform.transform(cause)).thenThrow(e);

        future.error(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, times(failedTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testErrorLazyTransform() throws Exception {
        final LazyTransform<Throwable, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        when(transform.transform(cause)).thenReturn(f);

        final AsyncFuture<Object> transformed = future.error(transform);

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

        future.error(transform);

        verify(transform, times(failedTimes)).transform(cause);
        verify(async, times(failedTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledTransform() throws Exception {
        final Transform<Void, Object> transform = Mockito.mock(Transform.class);
        final Object transfomed = new Object();

        when(transform.transform(null)).thenReturn(transfomed);

        future.cancelled(transform);

        verify(transform, times(cancelledTimes)).transform(null);
        verify(async, never()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledTransformThrows() throws Exception {
        final Transform<Void, Object> transform = Mockito.mock(Transform.class);

        when(transform.transform(null)).thenThrow(e);

        future.cancelled(transform);

        verify(transform, times(cancelledTimes)).transform(null);
        verify(async, times(cancelledTimes)).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledLazyTransform() throws Exception {
        final LazyTransform<Void, Object> transform = Mockito.mock(LazyTransform.class);
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        when(transform.transform(null)).thenReturn(f);

        final AsyncFuture<Object> transformed = future.cancelled(transform);

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

        future.cancelled(transform);

        verify(async, times(cancelledTimes)).failed(any(TransformException.class));
    }
}