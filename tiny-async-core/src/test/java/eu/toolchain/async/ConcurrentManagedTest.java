package eu.toolchain.async;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import eu.toolchain.async.ConcurrentManaged.ValidBorrowed;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentManagedTest {
    private static final Object reference = new Object();
    private static final Throwable e = new Exception();
    private static final StackTraceElement[] stack = new StackTraceElement[0];

    private ConcurrentManaged<Object> underTest;

    @Mock
    private AsyncFramework async;
    @Mock
    private ManagedSetup<Object> setup;
    @Mock
    private Borrowed<Object> borrowed;
    @Mock
    private ManagedAction<Object, Object> action;
    @Mock
    private ResolvableFuture<Void> startFuture;
    @Mock
    private ResolvableFuture<Void> zeroLeaseFuture;
    @Mock
    private ResolvableFuture<Object> stopReferenceFuture;
    @Mock
    private ResolvableFuture<Void> stopFuture;
    @Mock
    private AsyncFuture<Object> future;
    @Mock
    private AsyncFuture<Object> f;
    @Mock
    private FutureFinished finished;
    @Mock
    private AsyncFuture<Void> transformed;
    @Mock
    private AsyncFuture<Void> errored;

    @Before
    public void setup() {
        underTest = spy(new ConcurrentManaged<Object>(async, setup, startFuture, zeroLeaseFuture, stopReferenceFuture,
                stopFuture));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewManaged() throws Exception {
        final ResolvableFuture<Object> startFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> zeroLeaseFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> stopReferenceFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> stopFuture = mock(ResolvableFuture.class);

        when(async.future()).thenReturn(startFuture).thenReturn(zeroLeaseFuture).thenReturn(stopReferenceFuture);

        final AtomicReference<LazyTransform<Object, Object>> transform1 = new AtomicReference<>();

        doAnswer(new Answer<AsyncFuture<Object>>() {
            @Override
            public AsyncFuture<Object> answer(InvocationOnMock invocation) throws Throwable {
                transform1.set(invocation.getArgumentAt(0, LazyTransform.class));
                return stopFuture;
            }
        }).when(zeroLeaseFuture).transform(any(LazyTransform.class));

        final AtomicReference<LazyTransform<Object, Object>> transform2 = new AtomicReference<>();

        doAnswer(new Answer<AsyncFuture<Object>>() {
            @Override
            public AsyncFuture<Object> answer(InvocationOnMock invocation) throws Throwable {
                transform2.set(invocation.getArgumentAt(0, LazyTransform.class));
                return stopFuture;
            }
        }).when(stopReferenceFuture).transform(any(LazyTransform.class));

        ConcurrentManaged.newManaged(async, setup);

        verify(async, times(3)).future();
        verify(zeroLeaseFuture).transform(any(LazyTransform.class));
        verify(setup, never()).destruct(reference);
        verify(stopReferenceFuture, never()).transform(any(LazyTransform.class));

        transform1.get().transform(null);

        verify(setup, never()).destruct(reference);
        verify(stopReferenceFuture).transform(any(LazyTransform.class));

        transform2.get().transform(reference);

        verify(setup).destruct(reference);
    }

    private void setupDoto(boolean valid, boolean throwing) throws Exception {
        doReturn(borrowed).when(underTest).borrow();
        doReturn(finished).when(borrowed).releasing();
        doReturn(valid).when(borrowed).isValid();
        doReturn(future).when(async).cancelled();
        doReturn(future).when(async).failed(e);
        doReturn(reference).when(borrowed).get();

        if (throwing) {
            doThrow(e).when(action).action(reference);
        } else {
            doReturn(f).when(action).action(reference);
        }

        doReturn(future).when(f).on(finished);
    }

    private void verifyDoto(boolean valid, boolean throwing) throws Exception {
        verify(underTest).borrow();
        verify(borrowed).isValid();
        verify(async, times(!valid ? 1 : 0)).cancelled();
        verify(async, times(throwing ? 1 : 0)).failed(e);
        verify(borrowed, times(valid ? 1 : 0)).get();
        verify(borrowed, times(valid && !throwing ? 1 : 0)).releasing();
        verify(borrowed, times(throwing ? 1 : 0)).release();
        verify(action, times(valid ? 1 : 0)).action(reference);
        verify(f, times(valid && !throwing ? 1 : 0)).on(finished);
    }

    @Test
    public void testDotoInvalid() throws Exception {
        setupDoto(false, false);
        assertEquals(future, underTest.doto(action));
        verifyDoto(false, false);
    }

    @Test
    public void testDotoValidThrows() throws Exception {
        setupDoto(true, true);
        assertEquals(future, underTest.doto(action));
        verifyDoto(true, true);
    }

    @Test
    public void testDoto() throws Exception {
        setupDoto(true, false);
        assertEquals(future, underTest.doto(action));
        verifyDoto(true, false);
    }

    private void setupBorrow(boolean set) throws Exception {
        doNothing().when(underTest).retain();
        doNothing().when(underTest).release();
        doReturn(stack).when(underTest).getStackTrace();
        underTest.reference.set(set ? reference : null);
    }

    private void verifyBorrow(boolean set) throws Exception {
        verify(underTest).retain();
        verify(underTest, times(set ? 0 : 1)).release();
        verify(underTest, times(set ? 1 : 0)).getStackTrace();
    }

    @Test
    public void testBorrowNotSet() throws Exception {
        setupBorrow(false);
        assertFalse(underTest.borrow().isValid());
        verifyBorrow(false);
    }

    @Test
    public void testBorrow() throws Exception {
        setupBorrow(true);
        assertTrue(underTest.borrow().isValid());
        verifyBorrow(true);
    }

    @Test
    public void testIfReady() {
        doReturn(true).when(startFuture).isDone();
        assertTrue(underTest.isReady());
        verify(startFuture).isDone();
    }

    @Test
    public void testIfReadyNot() {
        doReturn(false).when(startFuture).isDone();
        assertFalse(underTest.isReady());
        verify(startFuture).isDone();
    }

    /**
     * @param initial If the startup method has an initial state that will cause an initialization.
     * @param result
     * @param cancelled
     */
    @SuppressWarnings("unchecked")
    private void setupStart(boolean initial, final Object result, final boolean cancelled) {
        underTest.state.set(initial ? ConcurrentManaged.ManagedState.INITIALIZED
                : ConcurrentManaged.ManagedState.STARTED);

        final AsyncFuture<Object> constructor = mock(AsyncFuture.class);

        doReturn(constructor).when(setup).construct();

        doAnswer(new Answer<AsyncFuture<Void>>() {
            @Override
            public AsyncFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                final FutureDone<Void> done = invocation.getArgumentAt(0, FutureDone.class);

                if (cancelled) {
                    done.cancelled();
                } else {
                    done.resolved(null);
                }

                return startFuture;
            }
        }).when(transformed).on(any(FutureDone.class));

        doAnswer(new Answer<AsyncFuture<Void>>() {
            @Override
            public AsyncFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                final FutureDone<Void> done = invocation.getArgumentAt(0, FutureDone.class);
                done.failed(e);
                return startFuture;
            }
        }).when(errored).on(any(FutureDone.class));

        doAnswer(new Answer<AsyncFuture<Void>>() {
            @Override
            public AsyncFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                final Transform<Object, Void> transform = invocation.getArgumentAt(0, Transform.class);

                if (cancelled)
                    return transformed;

                try {
                    transform.transform(result);
                } catch (Exception e) {
                    return errored;
                }

                return transformed;
            }
        }).when(constructor).transform(any(Transform.class));
    }

    private void verifyStart(boolean initial, final Object result, final boolean cancelled) {
        if (initial && result != null) {
            assertEquals(reference, underTest.reference.get());
        } else {
            assertEquals(null, underTest.reference.get());
        }

        if (initial) {
            if (cancelled) {
                verify(startFuture, never()).fail(e);
                verify(startFuture, never()).resolve(null);
                verify(startFuture).cancel();
            } else {
                verify(startFuture, times(result == null ? 1 : 0)).fail(e);
                verify(startFuture, times(result != null ? 1 : 0)).resolve(null);
                verify(startFuture, never()).cancel();
            }
        } else {
            verify(startFuture, never()).fail(e);
            verify(startFuture, never()).resolve(null);
            verify(startFuture, never()).cancel();
        }
    }

    @Test
    public void testStartWrongInitial() {
        setupStart(false, reference, true);
        assertEquals(startFuture, underTest.start());
        verifyStart(false, reference, true);
    }

    @Test
    public void testStartSetupNull() {
        setupStart(true, null, false);
        assertEquals(startFuture, underTest.start());
        verifyStart(true, null, false);
    }

    @Test
    public void testStartCancel() {
        setupStart(true, null, true);
        assertEquals(startFuture, underTest.start());
        verifyStart(true, null, true);
    }

    @Test
    public void testStart() {
        setupStart(true, reference, false);
        assertEquals(startFuture, underTest.start());
        verifyStart(true, reference, false);
    }

    @Test
    public void testStopInvalidState() {
        underTest.state.set(ConcurrentManaged.ManagedState.STOPPED);
        underTest.reference.set(reference);
        assertEquals(stopFuture, underTest.stop());
        assertEquals(reference, underTest.reference.get());
        verify(underTest, never()).release();
    }

    @Test
    public void testStop() {
        underTest.state.set(ConcurrentManaged.ManagedState.STARTED);
        underTest.reference.set(reference);
        assertEquals(stopFuture, underTest.stop());
        assertNull(underTest.reference.get());
        verify(underTest).release();
    }

    @Test
    public void testRetainRelease() {
        assertEquals(1, underTest.leases.get());
        underTest.retain();
        assertEquals(2, underTest.leases.get());
        underTest.release();
        assertEquals(1, underTest.leases.get());
    }

    @Test
    public void testZeroLeaseFutureResolve() {
        assertEquals(1, underTest.leases.get());
        verify(zeroLeaseFuture, never()).resolve(null);
        underTest.release();
        verify(zeroLeaseFuture, times(1)).resolve(null);
        underTest.retain();
        underTest.release();
        /* multiple invocations are expected due to the contract of ResolvableFuture#resolve() */
        verify(zeroLeaseFuture, times(2)).resolve(null);
    }

    @Test
    public void testToString() {
        assertEquals("Managed(INITIALIZED, null)", underTest.toString());
    }

    @Test
    public void testToStringTracing() {
        final ConcurrentManaged.ValidBorrowed<Object> b1 = mock(ConcurrentManaged.ValidBorrowed.class);
        final List<ValidBorrowed<Object>> traces = new ArrayList<>();
        traces.add(b1);

        doReturn(stack).when(b1).stack();

        assertNotNull(underTest.toStringTracing(reference, traces));
    }

    @Test
    public void testInvalidBorrow() throws Exception {
        final ConcurrentManaged.InvalidBorrowed<Object> invalid = new ConcurrentManaged.InvalidBorrowed<>();
        ConcurrentManaged.InvalidBorrowed.FINISHED.finished();

        // do nothing implementations
        invalid.close();
        invalid.release();

        assertEquals(ConcurrentManaged.InvalidBorrowed.FINISHED, invalid.releasing());
        assertFalse(invalid.isValid());

        try {
            invalid.get();
            fail("should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("invalid"));
        }
    }

    @Test
    public void testValidBorrowedBasics() throws Exception {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = new ValidBorrowed<Object>(managed, async, reference, stack);

        assertEquals(reference, valid.get());
        assertArrayEquals(stack, valid.stack());
    }

    @Test
    public void testValidBorrowedRelease() throws Exception {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = new ValidBorrowed<Object>(managed, async, reference, stack);

        assertFalse(valid.released.get());
        verify(managed, never()).release();
        valid.release();
        assertTrue(valid.released.get());
        verify(managed, times(1)).release();
        valid.release();
        assertTrue(valid.released.get());
        verify(managed, times(1)).release();
    }

    @Test
    public void testValidBorrowedClose() throws Exception {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = spy(new ValidBorrowed<Object>(managed, async, reference, stack));

        doNothing().when(valid).release();
        valid.close();
        verify(valid).release();
    }

    @Test
    public void testReleasing() throws Exception {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = spy(new ValidBorrowed<Object>(managed, async, reference, stack));

        doNothing().when(valid).release();
        valid.releasing().finished();
        verify(valid).release();
    }

    @Test
    public void testFinalizeDoNothing() throws Throwable {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = spy(new ValidBorrowed<Object>(managed, async, reference, stack));

        final AsyncCaller caller = mock(AsyncCaller.class);

        doReturn(caller).when(async).caller();
        valid.released.set(true);
        valid.finalize();
        verify(async, never()).caller();
        verify(caller, never()).leakedManagedReference(reference, stack);
    }

    @Test
    public void testFinalizeReportLeak() throws Throwable {
        final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
        final ValidBorrowed<Object> valid = spy(new ValidBorrowed<Object>(managed, async, reference, stack));

        final AsyncCaller caller = mock(AsyncCaller.class);

        doReturn(caller).when(async).caller();
        valid.finalize();
        verify(async).caller();
        verify(caller).leakedManagedReference(reference, stack);
    }
}