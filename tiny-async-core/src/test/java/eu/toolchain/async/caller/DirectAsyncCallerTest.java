package eu.toolchain.async.caller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
import eu.toolchain.async.StreamCollector;

public class DirectAsyncCallerTest {
    private static final Object reference = mock(Object.class);
    private static final RuntimeException e = new RuntimeException();

    private AtomicLong internalErrors;
    private DirectAsyncCaller caller;
    private FutureDone<Object> done;
    private FutureDone<Object> throwingDone;

    private FutureFinished finished;
    private FutureFinished throwingFinished;

    private FutureCancelled cancelled;
    private FutureCancelled throwingCancelled;

    private FutureFailed failed;
    private FutureFailed throwingFailed;

    private FutureResolved<Object> resolved;
    private FutureResolved<Object> throwingResolved;

    private StreamCollector<Object, Object> streamCollector;
    private StreamCollector<Object, Object> throwingStreamCollector;

    private String errorMessage;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        internalErrors = new AtomicLong();

        errorMessage = null;

        caller = new DirectAsyncCaller() {
            @Override
            protected void internalError(String what, Throwable e) {
                internalErrors.incrementAndGet();
                errorMessage = what;
            }
        };

        done = mock(FutureDone.class);

        throwingDone = mock(FutureDone.class);
        doThrow(e).when(throwingDone).cancelled();
        doThrow(e).when(throwingDone).resolved(reference);
        doThrow(e).when(throwingDone).failed(any(Throwable.class));

        finished = mock(FutureFinished.class);
        throwingFinished = mock(FutureFinished.class);
        doThrow(e).when(throwingFinished).finished();

        cancelled = mock(FutureCancelled.class);
        throwingCancelled = mock(FutureCancelled.class);
        doThrow(e).when(throwingCancelled).cancelled();

        failed = mock(FutureFailed.class);
        throwingFailed = mock(FutureFailed.class);
        doThrow(e).when(throwingFailed).failed(e);

        resolved = mock(FutureResolved.class);
        throwingResolved = mock(FutureResolved.class);
        doThrow(e).when(throwingResolved).resolved(reference);

        streamCollector = mock(StreamCollector.class);
        throwingStreamCollector = mock(StreamCollector.class);
        doThrow(e).when(throwingStreamCollector).resolved(reference);
        doThrow(e).when(throwingStreamCollector).cancelled();
        doThrow(e).when(throwingStreamCollector).failed(e);

        doReturn("foo").when(reference).toString();
    }

    @Test
    public void testIsThreaded() {
        assertEquals(false, caller.isThreaded());
    }

    @Test
    public void testResolveFutureDone() throws Exception {
        caller.resolveFutureDone(done, reference);
        verify(done).resolved(reference);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.resolveFutureDone(throwingDone, reference);
        verify(throwingDone).resolved(reference);
        assertEquals(1, internalErrors.get());
        assertEquals("FutureDone#resolved(T)", errorMessage);
    }

    @Test
    public void testCancelFutureDone() throws Exception {
        caller.cancelFutureDone(done);
        verify(done).cancelled();
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.cancelFutureDone(throwingDone);
        verify(throwingDone).cancelled();
        assertEquals(1, internalErrors.get());
        assertEquals("FutureDone#cancelled()", errorMessage);
    }

    @Test
    public void testFailFutureDone() throws Exception {
        caller.failFutureDone(done, e);
        verify(done).failed(e);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.failFutureDone(throwingDone, e);
        verify(throwingDone).failed(e);
        assertEquals(1, internalErrors.get());
        assertEquals("FutureDone#failed(Throwable)", errorMessage);
    }

    @Test
    public void testRunFutureFinished() throws Exception {
        caller.runFutureFinished(finished);
        verify(finished).finished();
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.runFutureFinished(throwingFinished);
        verify(throwingFinished).finished();
        assertEquals(1, internalErrors.get());
        assertEquals("FutureFinished#finished()", errorMessage);
    }

    @Test
    public void testRunFutureCancelled() throws Exception {
        caller.runFutureCancelled(cancelled);
        verify(cancelled).cancelled();
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.runFutureCancelled(throwingCancelled);
        verify(throwingCancelled).cancelled();
        assertEquals(1, internalErrors.get());
        assertEquals("FutureCancelled#cancelled()", errorMessage);
    }

    @Test
    public void testRunFutureFailed() throws Exception {
        caller.runFutureFailed(failed, e);
        verify(failed).failed(e);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.runFutureFailed(throwingFailed, e);
        verify(throwingFailed).failed(e);
        assertEquals(1, internalErrors.get());
        assertEquals("FutureFailed#failed(Throwable)", errorMessage);
    }

    @Test
    public void testRunFutureResolved() throws Exception {
        caller.runFutureResolved(resolved, reference);
        verify(resolved).resolved(reference);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.runFutureResolved(throwingResolved, reference);
        verify(throwingResolved).resolved(reference);
        assertEquals(1, internalErrors.get());
        assertEquals("FutureResolved#resolved(T)", errorMessage);
    }

    @Test
    public void testResolveStreamCollector() throws Exception {
        caller.resolveStreamCollector(streamCollector, reference);
        verify(streamCollector).resolved(reference);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.resolveStreamCollector(throwingStreamCollector, reference);
        verify(throwingStreamCollector).resolved(reference);
        assertEquals(1, internalErrors.get());
        assertEquals("StreamCollector#resolved(T)", errorMessage);
    }

    @Test
    public void testFailStreamCollector() throws Exception {
        caller.failStreamCollector(streamCollector, e);
        verify(streamCollector).failed(e);
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.failStreamCollector(throwingStreamCollector, e);
        verify(throwingStreamCollector).failed(e);
        assertEquals(1, internalErrors.get());
        assertEquals("StreamCollector#failed(Throwable)", errorMessage);
    }

    @Test
    public void testCancelStreamCollector() throws Exception {
        caller.cancelStreamCollector(streamCollector);
        verify(streamCollector).cancelled();
        assertEquals(0, internalErrors.get());
        assertEquals(null, errorMessage);

        caller.cancelStreamCollector(throwingStreamCollector);
        verify(throwingStreamCollector).cancelled();
        assertEquals(1, internalErrors.get());
        assertEquals("StreamCollector#cancel()", errorMessage);
    }

    @Test
    public void testLeakedManagedReferenceEmptyStack() {
        final StackTraceElement[] empty = new StackTraceElement[0];
        caller.leakedManagedReference(reference, empty);
        assertEquals(1, internalErrors.get());
        assertEquals("reference foo leaked @ unknown", errorMessage);
    }

    @Test
    public void testLeakedManagedReferenceUnknownStack() {
        final StackTraceElement[] unknown = null;
        caller.leakedManagedReference(reference, unknown);
        assertEquals(1, internalErrors.get());
        assertEquals("reference foo leaked @ unknown", errorMessage);
    }

    @Test
    public void testLeakedManagedReferencePopulatedStack() {
        final StackTraceElement[] populated = new StackTraceElement[2];
        populated[0] = new StackTraceElement("SomeClass", "method", "file", 0);
        populated[1] = new StackTraceElement("SomeOtherClass", "method", "file", 0);
        caller.leakedManagedReference(reference, populated);
        assertEquals(1, internalErrors.get());
        assertEquals("reference foo leaked @ SomeClass.method (file:0)\n  SomeOtherClass.method (file:0)", errorMessage);
    }
}