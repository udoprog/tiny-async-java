package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class ConcurrentResolvableFutureTest {
    private static final Object result = new Object();
    private static final Throwable cause = new Exception();

    private AsyncFuture<Object> other;
    private FutureDone<Object> done;
    private FutureCancelled cancelled;
    private FutureFinished finished;
    private FutureResolved<Object> resolved;
    private FutureFailed failed;

    private AsyncFramework async;
    private AsyncCaller caller;
    private ConcurrentResolvableFuture.S sync;
    private ConcurrentResolvableFuture<Object> future;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        other = mock(AsyncFuture.class);
        done = mock(FutureDone.class);
        cancelled = mock(FutureCancelled.class);
        finished = mock(FutureFinished.class);
        resolved = mock(FutureResolved.class);
        failed = mock(FutureFailed.class);
        async = mock(AsyncFramework.class);
        caller = mock(AsyncCaller.class);
        sync = mock(ConcurrentResolvableFuture.S.class);
        future = new ConcurrentResolvableFuture<>(async, caller, sync);
    }

    private void verifyEndState(int resolved, int failed, int cancelled) {
        verify(caller, times(resolved)).resolve(done, result);
        verify(caller, times(failed)).fail(done, cause);
        verify(caller, times(cancelled)).cancel(done);
    }

    @Test
    public void testIsResolved() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.complete(ConcurrentResolvableFuture.RESOLVED, result)).thenReturn(true);

        assertFalse(future.isResolved());
        assertFalse(future.isFailed());
        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        assertTrue(future.resolve(result));

        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);

        assertTrue(future.isResolved());
        assertFalse(future.isFailed());
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testIsFailed() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.complete(ConcurrentResolvableFuture.FAILED, cause)).thenReturn(true);

        assertFalse(future.isResolved());
        assertFalse(future.isFailed());
        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        assertTrue(future.fail(cause));

        when(sync.state()).thenReturn(ConcurrentResolvableFuture.FAILED);

        assertFalse(future.isResolved());
        assertTrue(future.isFailed());
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testIsCancelled() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.complete(ConcurrentResolvableFuture.CANCELLED)).thenReturn(true);

        assertFalse(future.isResolved());
        assertFalse(future.isFailed());
        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        assertTrue(future.cancel());

        when(sync.state()).thenReturn(ConcurrentResolvableFuture.CANCELLED);

        assertFalse(future.isResolved());
        assertFalse(future.isFailed());
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testResolveAlreadyDone() {
        when(sync.complete(ConcurrentResolvableFuture.RESOLVED, result)).thenReturn(false);
        assertFalse(future.resolve(result));
        verifyEndState(0, 0, 0);
    }

    @Test
    public void testResolveFireCallbacks() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        future.on(done);
        when(sync.complete(ConcurrentResolvableFuture.RESOLVED, result)).thenReturn(true);
        assertTrue(future.resolve(result));
        verify(caller).resolve(done, result);
        verifyEndState(1, 0, 0);
    }

    @Test
    public void testFailAlreadyDone() {
        when(sync.complete(ConcurrentResolvableFuture.FAILED, result)).thenReturn(false);
        assertFalse(future.fail(cause));
        verifyEndState(0, 0, 0);
    }

    @Test
    public void testFailFireCallbacks() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        future.on(done);
        when(sync.complete(ConcurrentResolvableFuture.FAILED, cause)).thenReturn(true);
        assertTrue(future.fail(cause));
        verifyEndState(0, 1, 0);
    }

    @Test
    public void testCancelAlreadyDone() {
        when(sync.complete(ConcurrentResolvableFuture.CANCELLED)).thenReturn(false);
        assertFalse(future.cancel());
        verifyEndState(0, 0, 0);
    }

    @Test
    public void testCancelFireCallbacks() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        future.on(done);
        when(sync.complete(ConcurrentResolvableFuture.CANCELLED)).thenReturn(true);
        assertTrue(future.cancel());
        verifyEndState(0, 0, 1);
    }

    private void verifyBind(int cancel, int state, int poll) {
        verify(other, times(cancel)).cancel();
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
    }

    @Test
    public void testBind() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        assertEquals(future, future.bind(other));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.AsyncFutureCB);
        verifyBind(0, 1, 0);
    }

    @Test
    public void testBindLateEndState() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        future.callbacks = null;
        assertEquals(future, future.bind(other));
        verifyBind(1, 1, 1);
    }

    @Test
    public void testBindCancelled() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        assertEquals(future, future.bind(other));
        verifyBind(1, 1, 0);
    }

    @Test
    public void testBindResolvedAfterAddFail() {
        /* non-cancel callback */
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        future.callbacks = null;
        assertEquals(future, future.bind(other));
        verifyBind(0, 1, 1);
    }

    @Test
    public void testBindResolved() {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        assertEquals(future, future.bind(other));
        verifyBind(0, 1, 0);
    }

    private void verifyOnFutureDone(int state, int poll, int resolve, int fail, int cancel) {
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
        verify(caller, times(resolve)).resolve(done, result);
        verify(caller, times(fail)).fail(done, cause);
        verify(caller, times(cancel)).cancel(done);
    }

    @Test
    public void testOnFutureDoneAddCallback() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(done));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.DoneCB);
        verifyOnFutureDone(1, 0, 0, 0, 0);
    }

    @Test
    public void testOnFutureDoneResolvedAfterAddFail() {
        /* non-cancel callback */
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        when(sync.result(ConcurrentResolvableFuture.RESOLVED)).thenReturn(result);
        future.callbacks = null;
        assertEquals(future, future.on(done));
        verifyOnFutureDone(1, 1, 1, 0, 0);
    }

    @Test
    public void testOnFutureDoneResolved() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        when(sync.result(ConcurrentResolvableFuture.RESOLVED)).thenReturn(result);
        assertEquals(future, future.on(done));
        verifyOnFutureDone(1, 0, 1, 0, 0);
    }

    @Test
    public void testOnFutureDoneFailed() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.FAILED);
        when(sync.result(ConcurrentResolvableFuture.FAILED)).thenReturn(cause);
        assertEquals(future, future.on(done));
        verifyOnFutureDone(1, 0, 0, 1, 0);
    }

    @Test
    public void testOnFutureDoneCancelled() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        assertEquals(future, future.on(done));
        verifyOnFutureDone(1, 0, 0, 0, 1);
    }

    @Test
    public void testOnFutureDoneIllegalState() throws Exception {
        when(sync.state()).thenReturn(Integer.MAX_VALUE);
        assertEquals(future, future.on(done));
        verifyOnFutureDone(1, 0, 0, 0, 0);
    }

    private void verifyOnFutureCancelled(int state, int poll, int cancel) {
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
        verify(caller, times(cancel)).cancel(cancelled);
    }

    @Test
    public void testOnFutureCancelledAddCallback() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(cancelled));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.CancelledCB);
        verifyOnFutureCancelled(1, 0, 0);
    }

    @Test
    public void testOnFutureCancelledCancelledAfterAddFail() {
        /* non-cancel callback */
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        future.callbacks = null;
        assertEquals(future, future.on(cancelled));
        verifyOnFutureCancelled(1, 1, 1);
    }

    @Test
    public void testOnFutureCancelledCancelled() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        assertEquals(future, future.on(cancelled));
        verifyOnFutureCancelled(1, 0, 1);
    }

    @Test
    public void testOnFutureCancelledOther() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        assertEquals(future, future.on(cancelled));
        verifyOnFutureCancelled(1, 0, 0);
    }

    private void verifyOnFutureFinished(int state, int poll, int finished) {
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
        verify(caller, times(finished)).finish(this.finished);
    }

    @Test
    public void testOnFutureFinishedAddCallback() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(finished));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.FinishedCB);
        verifyOnFutureFinished(1, 0, 0);
    }

    @Test
    public void testOnFutureFinishedFinishedAfterAddFail() {
        /* non-cancel callback */
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        future.callbacks = null;
        assertEquals(future, future.on(finished));
        verifyOnFutureFinished(1, 0, 1);
    }

    @Test
    public void testOnFutureFinishedFinished() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        assertEquals(future, future.on(finished));
        verifyOnFutureFinished(1, 0, 1);
    }

    private void verifyOnFutureResolved(int state, int poll, int resolved) {
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
        verify(caller, times(resolved)).resolve(this.resolved, result);
    }

    @Test
    public void testOnFutureResolvedAddCallback() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(resolved));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.ResolvedCB);
        verifyOnFutureResolved(1, 0, 0);
    }

    @Test
    public void testFutureResolvedAfterAddFail() {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        doReturn(result).when(sync).result(ConcurrentResolvableFuture.RESOLVED);
        future.callbacks = null;
        assertEquals(future, future.on(resolved));
        verifyOnFutureResolved(1, 1, 1);
    }

    @Test
    public void testOnFutureResolvedResolved() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        doReturn(result).when(sync).result(ConcurrentResolvableFuture.RESOLVED);
        assertEquals(future, future.on(resolved));
        verifyOnFutureResolved(1, 0, 1);
    }

    @Test
    public void testOnFutureResolvedOther() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.CANCELLED);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(resolved));
        verifyOnFutureResolved(1, 0, 0);
    }

    private void verifyOnFutureFailed(int state, int poll, int failed) {
        verify(sync, times(state)).state();
        verify(sync, times(poll)).poll();
        verify(caller, times(failed)).fail(this.failed, cause);
    }

    @Test
    public void testOnFutureFailedAddCallback() throws Exception {
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(failed));
        assertTrue(future.callbacks.get(0) instanceof ConcurrentResolvableFuture.FailedCB);
        verifyOnFutureFailed(1, 0, 0);
    }

    @Test
    public void testOnFutureCFailedFailedAfterAddFail() {
        /* non-cancel callback */
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RUNNING);
        when(sync.poll()).thenReturn(ConcurrentResolvableFuture.FAILED);
        doReturn(cause).when(sync).result(ConcurrentResolvableFuture.FAILED);
        future.callbacks = null;
        assertEquals(future, future.on(failed));
        verifyOnFutureFailed(1, 1, 1);
    }

    @Test
    public void testOnFutureFailedFailed() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.FAILED);
        doReturn(cause).when(sync).result(ConcurrentResolvableFuture.FAILED);
        assertEquals(future, future.on(failed));
        verifyOnFutureFailed(1, 0, 1);
    }

    @Test
    public void testOnFutureFailedOther() throws Exception {
        // resolve
        when(sync.state()).thenReturn(ConcurrentResolvableFuture.RESOLVED);
        doThrow(new RuntimeException()).when(sync).result(anyInt());
        assertEquals(future, future.on(failed));
        verifyOnFutureFailed(1, 0, 0);
    }
}