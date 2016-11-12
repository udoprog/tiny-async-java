package eu.toolchain.concurrent.concurrent;

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

import com.google.common.collect.ImmutableList;
import eu.toolchain.concurrent.Borrowed;
import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.FutureFramework;
import eu.toolchain.concurrent.concurrent.ConcurrentManaged.ValidBorrowed;
import eu.toolchain.concurrent.immediate.ImmediateCancelled;
import eu.toolchain.concurrent.immediate.ImmediateFailed;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentManagedTest {
  private static final Object reference = new Object();
  private static final RuntimeException e = new RuntimeException();
  private static final StackTraceElement[] stack = new StackTraceElement[0];

  private ConcurrentManaged<Object> underTest;

  @Mock
  private FutureFramework async;
  @Mock
  private FutureCaller caller;
  @Mock
  private Supplier<? extends CompletionStage<Object>> setup;
  @Mock
  private Function<? super Object, ? extends CompletionStage<Void>> teardown;
  @Mock
  private Borrowed<Object> borrowed;
  @Mock
  private Function<Object, CompletionStage<Object>> action;
  @Mock
  private CompletableFuture<Void> startFuture;
  @Mock
  private CompletableFuture<Void> zeroLeaseFuture;
  @Mock
  private CompletableFuture<Object> stopReferenceFuture;
  @Mock
  private CompletableFuture<Void> stopFuture;
  @Mock
  private CompletionStage<Object> future;
  @Mock
  private CompletionStage<Object> f;
  @Mock
  private CompletionStage<Void> transformed;
  @Mock
  private CompletionStage<Void> errored;

  @Before
  public void setup() {
    underTest = spy(new ConcurrentManaged<>(caller, setup, startFuture, zeroLeaseFuture,
        stopReferenceFuture, stopFuture));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNewManaged() throws Exception {
    final CompletableFuture<Object> startFuture = mock(CompletableFuture.class);
    final CompletableFuture<Object> zeroLeaseFuture = mock(CompletableFuture.class);
    final CompletableFuture<Object> stopReferenceFuture = mock(CompletableFuture.class);
    final CompletableFuture<Object> stopFuture = mock(CompletableFuture.class);

    when(async.future())
        .thenReturn(startFuture)
        .thenReturn(zeroLeaseFuture)
        .thenReturn(stopReferenceFuture);

    final AtomicReference<Function<Object, CompletionStage<Object>>> transform1 =
        new AtomicReference<>();

    doAnswer(new Answer<CompletionStage<Object>>() {
      @Override
      public CompletionStage<Object> answer(InvocationOnMock invocation) throws Throwable {
        transform1.set(invocation.getArgumentAt(0, Function.class));
        return stopFuture;
      }
    })
        .when(zeroLeaseFuture)
        .thenCompose((Function<Object, CompletionStage<Object>>) any(Function.class));

    final AtomicReference<Function<Object, CompletionStage<Object>>> transform2 =
        new AtomicReference<>();

    doAnswer(new Answer<CompletionStage<Object>>() {
      @Override
      public CompletionStage<Object> answer(InvocationOnMock invocation) throws Throwable {
        transform2.set(invocation.getArgumentAt(0, Function.class));
        return stopFuture;
      }
    })
        .when(stopReferenceFuture)
        .thenCompose((Function<Object, CompletionStage<Object>>) any(Function.class));

    ConcurrentManaged.newManaged(async, caller, setup, teardown);

    verify(async, times(3)).future();
    verify(zeroLeaseFuture).thenCompose(
        (Function<Object, CompletionStage<Object>>) any(Function.class));
    verify(teardown, never()).apply(reference);
    verify(stopReferenceFuture, never()).thenCompose(
        (Function<Object, CompletionStage<Object>>) any(Function.class));

    transform1.get().apply(null);

    verify(stopReferenceFuture).thenCompose(
        (Function<Object, CompletionStage<Object>>) any(Function.class));

    transform2.get().apply(reference);

    verify(teardown).apply(reference);
  }

  private void setupDoto(boolean valid, boolean throwing) throws Exception {
    doReturn(borrowed).when(underTest).borrow();
    doReturn(valid).when(borrowed).isValid();
    doReturn(future).when(async).cancelled();
    doReturn(future).when(async).failed(e);
    doReturn(reference).when(borrowed).get();

    if (throwing) {
      doThrow(e).when(action).apply(reference);
    } else {
      doReturn(f).when(action).apply(reference);
    }

    doReturn(future).when(f).whenFinished(any(Runnable.class));
  }

  private void verifyDoto(boolean valid, boolean throwing) throws Exception {
    verify(underTest).borrow();
    verify(borrowed).isValid();
    verify(borrowed, times(valid ? 1 : 0)).get();
    verify(borrowed, times(throwing ? 1 : 0)).release();
    verify(action, times(valid ? 1 : 0)).apply(reference);
    verify(f, times(valid && !throwing ? 1 : 0)).whenFinished(any(Runnable.class));
  }

  @Test
  public void testDotoInvalid() throws Exception {
    setupDoto(false, false);
    assertEquals(new ImmediateCancelled<>(caller), underTest.doto(action));
    verifyDoto(false, false);
  }

  @Test
  public void testDotoValidThrows() throws Exception {
    setupDoto(true, true);
    assertEquals(new ImmediateFailed<>(caller, e), underTest.doto(action));
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
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private void setupStart(
      boolean initial, final Object result, final boolean cancelled, final boolean constructThrows
  ) throws Exception {
    underTest.state.set(initial ? ConcurrentManaged.ManagedState.INITIALIZED
        : ConcurrentManaged.ManagedState.STARTED);

    final CompletionStage<Object> constructor = mock(CompletionStage.class);

    doReturn(startFuture).when(async).failed(e);

    if (constructThrows) {
      doThrow(e).when(setup).get();
    } else {
      doReturn(constructor).when(setup).get();
    }

    doAnswer(new Answer<CompletionStage<Void>>() {
      @Override
      public CompletionStage<Void> answer(InvocationOnMock invocation) throws Throwable {
        final CompletionHandle<Void> done = invocation.getArgumentAt(0, CompletionHandle.class);

        if (cancelled) {
          done.cancelled();
        } else {
          done.resolved(null);
        }

        return startFuture;
      }
    }).when(transformed).handle(any(CompletionHandle.class));

    doAnswer(new Answer<CompletionStage<Void>>() {
      @Override
      public CompletionStage<Void> answer(InvocationOnMock invocation) throws Throwable {
        final CompletionHandle<Void> done = invocation.getArgumentAt(0, CompletionHandle.class);
        done.failed(e);
        return startFuture;
      }
    }).when(errored).handle(any(CompletionHandle.class));

    doAnswer(new Answer<CompletionStage<Void>>() {
      @Override
      public CompletionStage<Void> answer(InvocationOnMock invocation) throws Throwable {
        final Function<Object, Void> transform = invocation.getArgumentAt(0, Function.class);

        if (cancelled) {
          return transformed;
        }

        try {
          transform.apply(result);
        } catch (Exception e) {
          return errored;
        }

        return transformed;
      }
    }).when(constructor).thenApply((Function<Object, Object>) any(Function.class));
  }

  @Test
  public void testStartConstructThrows() throws Exception {
    setupStart(true, null, false, true);
    assertEquals(new ImmediateFailed<>(caller, e), underTest.start());

    assertEquals(null, underTest.reference.get());

    verify(startFuture, never()).fail(e);
    verify(startFuture, never()).complete(null);
    verify(startFuture, never()).cancel();
  }

  @Test
  public void testStartWrongInitial() throws Exception {
    setupStart(false, reference, true, false);
    assertEquals(startFuture, underTest.start());

    assertEquals(null, underTest.reference.get());

    verify(startFuture, never()).fail(e);
    verify(startFuture, never()).complete(null);
    verify(startFuture, never()).cancel();
  }

  @Test
  public void testStartSetupNull() throws Exception {
    setupStart(true, null, false, false);
    assertEquals(startFuture, underTest.start());

    assertEquals(null, underTest.reference.get());

    verify(startFuture).fail(e);
    verify(startFuture, never()).complete(null);
    verify(startFuture, never()).cancel();
  }

  @Test
  public void testStartCancel() throws Exception {
    setupStart(true, null, true, false);
    assertEquals(startFuture, underTest.start());

    assertEquals(null, underTest.reference.get());

    verify(startFuture, never()).fail(e);
    verify(startFuture, never()).complete(null);
    verify(startFuture).cancel();
  }

  @Test
  public void testStart() throws Exception {
    setupStart(true, reference, false, false);
    assertEquals(startFuture, underTest.start());

    assertEquals(reference, underTest.reference.get());

    verify(startFuture, never()).fail(e);
    verify(startFuture).complete(null);
    verify(startFuture, never()).cancel();
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
    verify(zeroLeaseFuture, never()).complete(null);
    underTest.release();
    verify(zeroLeaseFuture, times(1)).complete(null);
    underTest.retain();
    underTest.release();
        /* multiple invocations are expected due to the contract of CompletableFuture#complete() */
    verify(zeroLeaseFuture, times(2)).complete(null);
  }

  @Test
  public void testToString() {
    assertEquals("Managed(INITIALIZED, null)", underTest.toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testToStringTracing() {
    assertNotNull(underTest.toStringTracing(reference,
        ImmutableList.of(underTest.new ValidBorrowed(reference, stack))));
  }

  @Test
  public void testInvalidBorrow() throws Exception {
    final ConcurrentManaged.InvalidBorrowed<Object> invalid =
        new ConcurrentManaged.InvalidBorrowed<>();
    // do nothing implementations
    invalid.close();
    invalid.release();

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
    final ValidBorrowed valid = underTest.new ValidBorrowed(reference, stack);

    assertEquals(reference, valid.get());
    assertArrayEquals(stack, valid.stack());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testValidBorrowedRelease() throws Exception {
    final ValidBorrowed valid = underTest.new ValidBorrowed(reference, stack);

    assertFalse(valid.released.get());
    verify(underTest, never()).release();
    valid.release();
    assertTrue(valid.released.get());
    verify(underTest, times(1)).release();
    valid.release();
    assertTrue(valid.released.get());
    verify(underTest, times(1)).release();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testValidBorrowedClose() throws Exception {
    final ConcurrentManaged<Object> managed = mock(ConcurrentManaged.class);
    final ValidBorrowed valid = spy(managed.new ValidBorrowed(reference, stack));

    doNothing().when(valid).release();
    valid.close();
    verify(valid).release();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFinalizeDoNothing() throws Throwable {
    final ValidBorrowed valid = spy(underTest.new ValidBorrowed(reference, stack));

    valid.released.set(true);
    valid.finalize();
    verify(caller, never()).referenceLeaked(reference, stack);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFinalizeReportLeak() throws Throwable {
    final ValidBorrowed valid = spy(underTest.new ValidBorrowed(reference, stack));

    valid.finalize();
    verify(caller).referenceLeaked(reference, stack);
  }
}
