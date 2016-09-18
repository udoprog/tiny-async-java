package eu.toolchain.async.concurrent;

import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentResolvableFutureTest {
    private static final Exception cause = new Exception();
    private static final long duration = 42;
    private static final TimeUnit unit = TimeUnit.NANOSECONDS;

    @Mock
    private From result;

    @Mock
    private ConcurrentResolvableFuture.Sync sync;

    @Mock
    private AsyncFramework async;

    @Mock
    private AsyncCaller caller;

    @Mock
    private Runnable runnable;

    @Mock
    private FutureDone<From> done;

    @Mock
    private AsyncFuture<?> other;

    @Mock
    private FutureCancelled cancelled;

    @Mock
    private FutureResolved<From> resolved;

    @Mock
    private FutureFailed failed;

    @Mock
    private FutureFinished finished;

    private ConcurrentResolvableFuture<From> future;

    @Before
    public void setup() {
        doAnswer(invocation -> {
            Runnable runnable1 = invocation.getArgumentAt(0, Runnable.class);
            runnable1.run();
            return null;
        }).when(caller).execute(any(Runnable.class));

        future = spy(new ConcurrentResolvableFuture<From>(async, caller, sync));
    }

    @Test
    public void testResolve1() {
        doReturn(true).when(sync).setResult(ConcurrentResolvableFuture.RESOLVED, result);
        doNothing().when(future).run();

        assertTrue(future.resolve(result));

        verify(sync).setResult(ConcurrentResolvableFuture.RESOLVED, result);
        verify(future).run();
    }

    @Test
    public void testResolve2() {
        doReturn(false).when(sync).setResult(ConcurrentResolvableFuture.RESOLVED, result);
        doNothing().when(future).run();

        assertFalse(future.resolve(result));

        verify(sync).setResult(ConcurrentResolvableFuture.RESOLVED, result);
        verify(future, never()).run();
    }

    @Test
    public void testFail1() {
        doReturn(true).when(sync).setResult(ConcurrentResolvableFuture.FAILED, cause);
        doNothing().when(future).run();

        assertTrue(future.fail(cause));

        verify(sync).setResult(ConcurrentResolvableFuture.FAILED, cause);
        verify(future).run();
    }

    @Test
    public void testFail2() {
        doReturn(false).when(sync).setResult(ConcurrentResolvableFuture.FAILED, cause);
        doNothing().when(future).run();

        assertFalse(future.fail(cause));

        verify(sync).setResult(ConcurrentResolvableFuture.FAILED, cause);
        verify(future, never()).run();
    }

    @Test
    public void testCancel1() {
        doReturn(true).when(sync).setResult(ConcurrentResolvableFuture.CANCELLED);
        doNothing().when(future).run();

        assertTrue(future.cancel(true));

        verify(sync).setResult(ConcurrentResolvableFuture.CANCELLED);
        verify(future).run();
    }

    @Test
    public void testCancel2() {
        doReturn(false).when(sync).setResult(ConcurrentResolvableFuture.CANCELLED);
        doNothing().when(future).run();

        assertFalse(future.cancel(true));

        verify(sync).setResult(ConcurrentResolvableFuture.CANCELLED);
        verify(future, never()).run();
    }

    @Test
    public void testDefaultCancel() {
        doReturn(true).when(future).cancel(false);

        assertTrue(future.cancel());

        verify(future).cancel(false);
    }

    @Test
    public void testBind() {
        doReturn(runnable).when(future).otherRunnable(other);
        doReturn(true).when(future).add(runnable);

        assertEquals(future, future.bind(other));

        verify(future).otherRunnable(other);
        verify(future).add(runnable);
        verify(runnable, never()).run();
    }

    @Test
    public void testBindDirect() {
        doReturn(runnable).when(future).otherRunnable(other);
        doReturn(false).when(future).add(runnable);

        assertEquals(future, future.bind(other));

        verify(future).otherRunnable(other);
        verify(future).add(runnable);
        verify(runnable).run();
    }

    @Test
    public void testOnDone() {
        doReturn(runnable).when(future).doneRunnable(done);
        doReturn(true).when(future).add(runnable);

        assertEquals(future, future.on(done));

        verify(future).doneRunnable(done);
        verify(future).add(runnable);
        verify(runnable, never()).run();
    }

    @Test
    public void testOnDoneDirect() {
        doReturn(runnable).when(future).doneRunnable(done);
        doReturn(false).when(future).add(runnable);

        assertEquals(future, future.on(done));

        verify(future).doneRunnable(done);
        verify(future).add(runnable);
        verify(runnable).run();
    }

    @Test
    public void testOtherRunnable1() {
        doReturn(ConcurrentResolvableFuture.CANCELLED).when(sync).poll();

        future.otherRunnable(other).run();

        verify(sync).poll();
        verify(other).cancel();
    }

    @Test
    public void testOtherRunnable2() {
        doReturn(~ConcurrentResolvableFuture.CANCELLED).when(sync).poll();

        future.otherRunnable(other).run();

        verify(sync).poll();
        verify(other, never()).cancel();
    }

    @Test
    public void testDoneRunnable1() {
        doReturn(ConcurrentResolvableFuture.FAILED).when(sync).poll();
        sync.result = cause;

        future.doneRunnable(done).run();

        verify(sync).poll();
        verify(caller, never()).resolve(done, result);
        verify(caller, never()).cancel(done);
        verify(caller).fail(done, cause);
    }

    @Test
    public void testDoneRunnable2() {
        doReturn(ConcurrentResolvableFuture.CANCELLED).when(sync).poll();

        future.doneRunnable(done).run();

        verify(sync).poll();
        verify(caller, never()).resolve(done, result);
        verify(caller).cancel(done);
        verify(caller, never()).fail(done, cause);
    }

    @Test
    public void testDoneRunnable3() {
        doReturn(ConcurrentResolvableFuture.RESOLVED).when(sync).poll();
        sync.result = result;

        future.doneRunnable(done).run();

        verify(sync).poll();
        verify(caller).resolve(done, result);
        verify(caller, never()).cancel(done);
        verify(caller, never()).fail(done, cause);
    }

    @Test
    public void testCancelledRunnable1() {
        doReturn(ConcurrentResolvableFuture.CANCELLED).when(sync).poll();

        future.cancelledRunnable(cancelled).run();

        verify(sync).poll();
        verify(caller).cancel(cancelled);
    }

    @Test
    public void testCancelledRunnable2() {
        doReturn(~ConcurrentResolvableFuture.CANCELLED).when(sync).poll();

        future.cancelledRunnable(cancelled).run();

        verify(sync).poll();
        verify(caller, never()).cancel(cancelled);
    }

    @Test
    public void testFinishedRunnable() {
        doReturn(0).when(sync).poll();

        future.finishedRunnable(finished).run();

        verify(sync, never()).poll();
        verify(caller).finish(finished);
    }

    @Test
    public void testResolvedRunnable1() {
        doReturn(ConcurrentResolvableFuture.RESOLVED).when(sync).poll();
        sync.result = result;

        future.resolvedRunnable(resolved).run();

        verify(sync).poll();
        verify(caller).resolve(resolved, result);
    }

    @Test
    public void testResolvedRunnable2() {
        doReturn(~ConcurrentResolvableFuture.RESOLVED).when(sync).poll();
        sync.result = result;

        future.resolvedRunnable(resolved).run();

        verify(sync).poll();
        verify(caller, never()).resolve(resolved, result);
    }

    @Test
    public void testFailedRunnable1() {
        doReturn(ConcurrentResolvableFuture.FAILED).when(sync).poll();
        sync.result = cause;

        future.failedRunnable(failed).run();

        verify(sync).poll();
        verify(caller).fail(failed, cause);
    }

    @Test
    public void testFailedRunnable2() {
        doReturn(~ConcurrentResolvableFuture.FAILED).when(sync).poll();
        sync.result = result;

        future.failedRunnable(failed).run();

        verify(sync).poll();
        verify(caller, never()).fail(failed, cause);
    }

    @Test
    public void testIsDone() {
        doReturn(true).when(sync).isDone();
        assertTrue(future.isDone());
        verify(sync).isDone();
    }

    @Test
    public void testIsResolved() {
        doReturn(true).when(sync).isResolved();
        assertTrue(future.isResolved());
        verify(sync).isResolved();
    }

    @Test
    public void testIsFailed() {
        doReturn(true).when(sync).isFailed();
        assertTrue(future.isFailed());
        verify(sync).isFailed();
    }

    @Test
    public void testIsCancelled() {
        doReturn(true).when(sync).isCancelled();
        assertTrue(future.isCancelled());
        verify(sync).isCancelled();
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        doReturn(result).when(sync).get();
        assertEquals(result, future.get());
        verify(sync).get();
    }

    @Test
    public void testGetNow() throws ExecutionException, InterruptedException {
        doReturn(result).when(sync).getNow();
        assertEquals(result, future.getNow());
        verify(sync).getNow();
    }

    @Test
    public void testGetTimed() throws ExecutionException, InterruptedException, TimeoutException {
        doReturn(result).when(sync).get(duration);
        assertEquals(result, future.get(duration, unit));
        verify(sync).get(duration);
    }

    private static interface To {
    }

    private static interface From {
    }
}
