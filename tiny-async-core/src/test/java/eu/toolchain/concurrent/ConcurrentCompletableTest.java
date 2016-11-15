package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
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
public class ConcurrentCompletableTest {
  private static final Exception cause = new Exception();

  @Mock
  private From result;
  @Mock
  private Caller caller;
  @Mock
  private Runnable runnable;
  @Mock
  private CompletionHandle<From> done;
  @Mock
  private Stage<?> other;
  @Mock
  private Runnable cancelled;
  @Mock
  private Consumer<From> resolved;
  @Mock
  private Consumer<Throwable> failed;
  @Mock
  private Runnable finished;
  @Mock
  private ConcurrentCompletable<To> toFuture;
  @Mock
  private ConcurrentCompletable<From> fromFuture;

  private ConcurrentCompletable<From> future;

  @Before
  public void setup() {
    future = spy(new ConcurrentCompletable<From>(caller));

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(caller).execute(any(Runnable.class));
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
    future.state.set(ConcurrentCompletable.COMPLETED);
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
    future.state.set(ConcurrentCompletable.FAILED);
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
    future.state.set(ConcurrentCompletable.CANCELLED);
    future.result = ConcurrentCompletable.CANCEL;

    doNothing().when(future).postComplete();
    assertFalse(future.cancel());
    verify(future, never()).postComplete();
  }

  @Test
  public void testHandle() {
    doReturn(runnable).when(future).doneRunnable(done);
    doReturn(true).when(future).add(runnable);

    assertEquals(future, future.whenDone(done));

    verify(future).doneRunnable(done);
    verify(future).add(runnable);
    verify(runnable, never()).run();
  }

  @Test
  public void testHandleDirect() {
    doReturn(runnable).when(future).doneRunnable(done);
    doReturn(false).when(future).add(runnable);

    assertEquals(future, future.whenDone(done));

    verify(future).doneRunnable(done);
    verify(future).add(runnable);
    verify(runnable).run();
  }

  @Test
  public void testDoneRunnable1() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;

    future.doneRunnable(done).run();

    verify(done, never()).completed(result);
    verify(done, never()).cancelled();
    verify(done).failed(cause);
  }

  @Test
  public void testDoneRunnable2() {
    future.state.set(ConcurrentCompletable.CANCELLED);
    future.result = ConcurrentCompletable.CANCEL;

    future.doneRunnable(done).run();

    verify(done, never()).completed(result);
    verify(done).cancelled();
    verify(done, never()).failed(cause);
  }

  @Test
  public void testDoneRunnable3() {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;

    future.doneRunnable(done).run();

    verify(done).completed(result);
    verify(done, never()).cancelled();
    verify(done, never()).failed(cause);
  }

  @Test
  public void testCancelledRunnable1() {
    future.state.set(ConcurrentCompletable.CANCELLED);
    future.result = ConcurrentCompletable.CANCEL;

    future.cancelledRunnable(cancelled).run();
    verify(cancelled).run();
  }

  @Test
  public void testCancelledRunnable2() {
    future.state.set(~ConcurrentCompletable.CANCELLED);

    future.cancelledRunnable(cancelled).run();
    verify(cancelled, never()).run();
  }

  @Test
  public void testResolvedRunnable1() {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;

    future.resolvedRunnable(resolved).run();
    verify(resolved).accept(result);
  }

  @Test
  public void testResolvedRunnable2() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;

    future.resolvedRunnable(resolved).run();

    verify(resolved, never()).accept(result);
  }

  @Test
  public void testFailedRunnable1() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;

    future.failedRunnable(failed).run();
    verify(failed).accept(cause);
  }

  @Test
  public void testFailedRunnable2() {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;

    future.failedRunnable(failed).run();
    verify(failed, never()).accept(cause);
  }

  @Test
  public void testIsDone() {
    future.result = result;
    assertTrue(future.isDone());
  }

  @Test
  public void testIsResolved() {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;
    assertTrue(future.isCompleted());
  }

  @Test
  public void testIsFailed() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;
    assertTrue(future.isFailed());
  }

  @Test
  public void testIsCancelled() {
    future.result = ConcurrentCompletable.CANCEL;
    assertTrue(future.isCancelled());
  }

  @Test
  public void testJoinNow() throws Exception {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;
    assertEquals(result, future.joinNow());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void verifyTransform() {
    verify(future).newFuture();
    verify(future).whenFinished(any(Runnable.class));
  }

  @Test
  public void thenApply() {
    @SuppressWarnings("unchecked") final Function<Object, Object> fn = mock(Function.class);

    doReturn(toFuture).when(future).newFuture();
    doReturn(toFuture).when(toFuture).whenCancelled(any(Runnable.class));

    assertEquals(toFuture, future.thenApply(fn));
    verifyTransform();
  }

  @Test
  public void thenCompose() {
    @SuppressWarnings("unchecked") final Function<Object, Stage<Object>> fn =
        mock(Function.class);

    doReturn(toFuture).when(future).newFuture();
    doReturn(toFuture).when(toFuture).whenCancelled(any(Runnable.class));

    assertEquals(toFuture, future.thenCompose(fn));
    verifyTransform();
  }

  @Test
  public void thenCatchFailed() {
    @SuppressWarnings("unchecked") final Function<Throwable, From> fn = mock(Function.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).whenCancelled(any(Runnable.class));

    assertEquals(fromFuture, future.thenApplyFailed(fn));
    verifyTransform();
  }

  @Test
  public void thenComposeFailed() {
    @SuppressWarnings("unchecked") final Function<Throwable, Stage<From>> fn =
        mock(Function.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).whenCancelled(any(Runnable.class));

    assertEquals(fromFuture, future.thenComposeFailed(fn));
    verifyTransform();
  }

  @Test
  public void thenCatchCancelled() {
    @SuppressWarnings("unchecked") final Supplier<From> fn = mock(Supplier.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).whenCancelled(any(Runnable.class));

    assertEquals(fromFuture, future.thenApplyCancelled(fn));
    verifyTransform();
  }

  @Test
  public void thenComposeCancelled() {
    @SuppressWarnings("unchecked") final Supplier<Stage<From>> fn = mock(Supplier.class);

    doReturn(fromFuture).when(future).newFuture();
    doReturn(fromFuture).when(fromFuture).whenCancelled(any(Runnable.class));

    assertEquals(fromFuture, future.thenComposeCancelled(fn));
    verifyTransform();
  }

  private interface To {
  }

  private interface From {
  }
}
