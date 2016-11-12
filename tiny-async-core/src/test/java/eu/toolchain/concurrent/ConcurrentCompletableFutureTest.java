package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentCompletableFutureTest {
  private static final Exception cause = new Exception();

  @Mock
  private From result;
  @Mock
  private FutureCaller caller;
  @Mock
  private Runnable runnable;
  @Mock
  private CompletionHandle<From> done;
  @Mock
  private CompletionStage<?> other;
  @Mock
  private Runnable cancelled;
  @Mock
  private Consumer<From> resolved;
  @Mock
  private Consumer<Throwable> failed;
  @Mock
  private Runnable finished;
  @Mock
  private CompletableFuture<To> toFuture;
  @Mock
  private CompletableFuture<From> fromFuture;

  private ConcurrentCompletableFuture<From> future;

  @Before
  public void setup() {
    future = spy(new ConcurrentCompletableFuture<From>(caller));
  }

  @Test
  public void testResolve1() {
    doNothing().when(future).postComplete();
    assertTrue(future.complete(result));
    verify(future).postComplete();
    assertNotNull(future.result);
  }

  @Test
  public void testResolve2() {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;

    doNothing().when(future).postComplete();
    assertFalse(future.complete(result));
    verify(future, never()).postComplete();
  }

  @Test
  public void testFail1() {
    doNothing().when(future).postComplete();
    assertTrue(future.fail(cause));
    verify(future).postComplete();
    assertNotNull(future.result);
  }

  @Test
  public void testFail2() {
    future.state.set(ConcurrentCompletableFuture.FAILED);
    future.result = cause;

    doNothing().when(future).postComplete();
    assertFalse(future.fail(cause));
    verify(future, never()).postComplete();
    assertNotNull(future.result);
  }

  @Test
  public void testCancel() {
    doNothing().when(future).postComplete();
    assertTrue(future.cancel());
    verify(future).postComplete();
  }

  @Test
  public void testCancel2() {
    future.state.set(ConcurrentCompletableFuture.CANCELLED);
    future.result = ConcurrentCompletableFuture.CANCEL;

    doNothing().when(future).postComplete();
    assertFalse(future.cancel());
    verify(future, never()).postComplete();
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
  public void testHandle() {
    doReturn(runnable).when(future).doneRunnable(done);
    doReturn(true).when(future).add(runnable);

    assertEquals(future, future.handle(done));

    verify(future).doneRunnable(done);
    verify(future).add(runnable);
    verify(runnable, never()).run();
  }

  @Test
  public void testHandleDirect() {
    doReturn(runnable).when(future).doneRunnable(done);
    doReturn(false).when(future).add(runnable);

    assertEquals(future, future.handle(done));

    verify(future).doneRunnable(done);
    verify(future).add(runnable);
    verify(runnable).run();
  }

  @Test
  public void testOtherRunnable1() {
    future.state.set(ConcurrentCompletableFuture.CANCELLED);
    future.otherRunnable(other).run();
    verify(other).cancel();
  }

  @Test
  public void testOtherRunnable2() {
    future.state.set(~ConcurrentCompletableFuture.CANCELLED);
    future.otherRunnable(other).run();
    verify(other, never()).cancel();
  }

  @Test
  public void testDoneRunnable1() {
    future.state.set(ConcurrentCompletableFuture.FAILED);
    future.result = cause;

    future.doneRunnable(done).run();

    verify(caller, never()).complete(done, result);
    verify(caller, never()).cancel(done);
    verify(caller).fail(done, cause);
  }

  @Test
  public void testDoneRunnable2() {
    future.state.set(ConcurrentCompletableFuture.CANCELLED);
    future.result = ConcurrentCompletableFuture.CANCEL;

    future.doneRunnable(done).run();

    verify(caller, never()).complete(done, result);
    verify(caller).cancel(done);
    verify(caller, never()).fail(done, cause);
  }

  @Test
  public void testDoneRunnable3() {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;

    future.doneRunnable(done).run();

    verify(caller).complete(done, result);
    verify(caller, never()).cancel(done);
    verify(caller, never()).fail(done, cause);
  }

  @Test
  public void testCancelledRunnable1() {
    future.state.set(ConcurrentCompletableFuture.CANCELLED);
    future.result = ConcurrentCompletableFuture.CANCEL;

    future.cancelledRunnable(cancelled).run();
    verify(caller).cancel(cancelled);
  }

  @Test
  public void testCancelledRunnable2() {
    future.state.set(~ConcurrentCompletableFuture.CANCELLED);

    future.cancelledRunnable(cancelled).run();
    verify(caller, never()).cancel(cancelled);
  }

  @Test
  public void testFinishedRunnable() {
    future.finishedRunnable(finished).run();
    verify(caller).finish(finished);
  }

  @Test
  public void testResolvedRunnable1() {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;

    future.resolvedRunnable(resolved).run();
    verify(caller).complete(resolved, result);
  }

  @Test
  public void testResolvedRunnable2() {
    future.state.set(ConcurrentCompletableFuture.FAILED);
    future.result = cause;

    future.resolvedRunnable(resolved).run();

    verify(caller, never()).complete(resolved, result);
  }

  @Test
  public void testFailedRunnable1() {
    future.state.set(ConcurrentCompletableFuture.FAILED);
    future.result = cause;

    future.failedRunnable(failed).run();
    verify(caller).fail(failed, cause);
  }

  @Test
  public void testFailedRunnable2() {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;

    future.failedRunnable(failed).run();
    verify(caller, never()).fail(failed, cause);
  }

  @Test
  public void testIsDone() {
    future.result = result;
    assertTrue(future.isDone());
  }

  @Test
  public void testIsResolved() {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;
    assertTrue(future.isCompleted());
  }

  @Test
  public void testIsFailed() {
    future.state.set(ConcurrentCompletableFuture.FAILED);
    future.result = cause;
    assertTrue(future.isFailed());
  }

  @Test
  public void testIsCancelled() {
    future.result = ConcurrentCompletableFuture.CANCEL;
    assertTrue(future.isCancelled());
  }

  @Test
  public void testJoinNow() throws Exception {
    future.state.set(ConcurrentCompletableFuture.COMPLETED);
    future.result = result;
    assertEquals(result, future.joinNow());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void verifyTransform(Class<? extends CompletionHandle> done) {
    verify(future).newFuture();
    verify(future).handle(any(done));
  }

  @Test
  public void thenApply() {
    @SuppressWarnings("unchecked") final Function<Object, Object> fn = mock(Function.class);

    doReturn(toFuture).when(future).newFuture();
    doReturn(toFuture).when(toFuture).bind(future);

    assertEquals(toFuture, future.thenApply(fn));
    verifyTransform(ThenApplyHelper.class);
  }

  @Test
  public void thenCompose() {
    @SuppressWarnings("unchecked") final Function<Object, CompletionStage<Object>> fn =
        mock(Function.class);

    doReturn(toFuture).when(future).newFuture();
    doReturn(toFuture).when(toFuture).bind(future);

    assertEquals(toFuture, future.thenCompose(fn));
    verifyTransform(ThenComposeHelper.class);
  }

  @Test
  public void thenCatchFailed() {
    @SuppressWarnings("unchecked") final Function<Throwable, From> fn = mock(Function.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).bind(future);

    assertEquals(fromFuture, future.thenCatchFailed(fn));
    verifyTransform(ThenCatchFailedHelper.class);
  }

  @Test
  public void thenComposeFailed() {
    @SuppressWarnings("unchecked") final Function<Throwable, CompletionStage<From>> fn =
        mock(Function.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).bind(future);

    assertEquals(fromFuture, future.thenComposeFailed(fn));
    verifyTransform(ThenComposeFailedHelper.class);
  }

  @Test
  public void thenCatchCancelled() {
    @SuppressWarnings("unchecked") final Supplier<From> fn = mock(Supplier.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).bind(future);

    assertEquals(fromFuture, future.thenCatchCancelled(fn));
    verifyTransform(ThenCatchCancelledHelper.class);
  }

  @Test
  public void thenComposeCancelled() {
    @SuppressWarnings("unchecked") final Supplier<CompletionStage<From>> fn = mock(Supplier.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).bind(future);

    assertEquals(fromFuture, future.thenComposeCancelled(fn));
    verifyTransform(TheComposeCancelledHelper.class);
  }

  private interface To {
  }

  private interface From {
  }
}
