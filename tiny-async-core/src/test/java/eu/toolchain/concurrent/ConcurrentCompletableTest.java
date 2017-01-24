package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ConcurrentCompletableTest {
  private static final RuntimeException cause = new RuntimeException();
  private static final RuntimeException cause2 = new RuntimeException();

  @Mock
  private From result;
  @Mock
  private To to;
  @Mock
  private Caller caller;
  @Mock
  private Runnable runnable;
  @Mock
  private Handle<From> done;
  @Mock
  private Stage<To> other;
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
  @Mock
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
    doReturn(runnable).when(future).handleRunnable(done);
    doReturn(true).when(future).add(runnable);

    assertEquals(future, future.handle(done));

    verify(future).handleRunnable(done);
    verify(future).add(runnable);
    verify(runnable, never()).run();
  }

  @Test
  public void testHandleDirect() {
    doReturn(runnable).when(future).handleRunnable(done);
    doReturn(false).when(future).add(runnable);

    assertEquals(future, future.handle(done));

    verify(future).handleRunnable(done);
    verify(future).add(runnable);
    verify(runnable).run();
  }

  @Test
  public void testDoneRunnable1() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;

    future.handleRunnable(done).run();

    verify(done, never()).completed(result);
    verify(done, never()).cancelled();
    verify(done).failed(cause);
  }

  @Test
  public void testDoneRunnable2() {
    future.state.set(ConcurrentCompletable.CANCELLED);
    future.result = ConcurrentCompletable.CANCEL;

    future.handleRunnable(done).run();

    verify(done, never()).completed(result);
    verify(done).cancelled();
    verify(done, never()).failed(cause);
  }

  @Test
  public void testDoneRunnable3() {
    future.state.set(ConcurrentCompletable.COMPLETED);
    future.result = result;

    future.handleRunnable(done).run();

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

    future.completedRunnable(resolved).run();
    verify(resolved).accept(result);
  }

  @Test
  public void testResolvedRunnable2() {
    future.state.set(ConcurrentCompletable.FAILED);
    future.result = cause;

    future.completedRunnable(resolved).run();

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
    verify(future).nextStage();
    verify(future).whenDone(any(Runnable.class));
  }

  @Test
  public void thenApply() {
    final Function<Object, Object> fn = mock(Function.class);

    doReturn(toFuture).when(future).nextStage();

    assertEquals(toFuture, future.thenApply(fn));
    verifyTransform();
  }

  @Test
  public void thenCompose() {
    final Function<Object, Stage<Object>> fn = mock(Function.class);

    doReturn(toFuture).when(future).nextStage();

    assertEquals(toFuture, future.thenCompose(fn));
    verifyTransform();
  }

  @Test
  public void thenApplyCaught() {
    final Function<Throwable, From> fn = mock(Function.class);

    doReturn(fromFuture).when(future).nextStage();

    assertEquals(fromFuture, future.thenApplyFailed(fn));
    verifyTransform();
  }

  @Test
  public void thenComposeCaught() {
    final Function<Throwable, Stage<From>> fn = mock(Function.class);

    doReturn(fromFuture).when(future).nextStage();

    assertEquals(fromFuture, future.thenComposeCaught(fn));
    verifyTransform();
  }

  @Test
  public void testThenApply() {
    final Function<From, To> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable<From>.ThenApply<To> helper =
      c.completable.new ThenApply<To>(toFuture, fn);

    doReturn(to).when(fn).apply(result);

    c.complete(result);
    helper.run();

    doThrow(cause).when(fn).apply(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, toFuture);
    order.verify(fn).apply(result);
    order.verify(toFuture).complete(to);
    order.verify(fn).apply(result);
    order.verify(toFuture).fail(cause);
    order.verify(toFuture).cancel();
    order.verify(toFuture).fail(cause);
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenCompose() {
    final Function<From, Stage<To>> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable<From>.ThenCompose<To> helper =
      c.completable.new ThenCompose<To>(toFuture, fn);

    doNothing().when(c.completable).handleStage(any(), eq(toFuture));

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, toFuture, c.completable);

    order.verify(c.completable).handleStage(any(), eq(toFuture));
    order.verify(toFuture).cancel();
    order.verify(toFuture).fail(cause);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenApplyFailed() {
    final Function<Throwable, From> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenApplyFailed helper =
      c.completable.new ThenApplyFailed(future, fn);

    doReturn(result).when(fn).apply(cause);

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();
    doThrow(cause2).when(fn).apply(cause);
    helper.run();

    final InOrder order = inOrder(fn, future);

    order.verify(future).complete(result);
    order.verify(future).cancel();
    order.verify(fn).apply(cause);
    order.verify(future).fail(cause2);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenComposeFailed() {
    final Function<Throwable, Stage<From>> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenComposeFailed helper =
      c.completable.new ThenComposeFailed(future, fn);

    doNothing().when(c.completable).handleStage(any(), eq(future));
    doReturn(result).when(fn).apply(cause);

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, future, c.completable);

    order.verify(future).complete(result);
    order.verify(future).cancel();
    order.verify(c.completable).handleStage(any(), eq(future));

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithCloser() {
    final Supplier<Stage<Void>> complete = Mockito.mock(Supplier.class);
    final Supplier<Stage<Void>> notComplete = Mockito.mock(Supplier.class);
    final Stage<Void> completeStage = Mockito.mock(Stage.class);
    final Stage<Void> notCompleteStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.WithCloser helper =
      c.completable.new WithCloser(future, complete, notComplete);

    doReturn(completeStage).when(complete).get();
    doReturn(notCompleteStage).when(notComplete).get();

    doReturn(next).when(completeStage).thenApply(any());
    doReturn(next).when(notCompleteStage).thenCancel();
    doReturn(next).when(notCompleteStage).thenFail(any());

    c.complete(result);
    helper.run();

    doThrow(cause).when(complete).get();
    helper.run();

    c.cancel();
    helper.run();

    doThrow(cause).when(notComplete).get();
    helper.run();

    doReturn(notCompleteStage).when(notComplete).get();
    c.fail(cause);
    helper.run();

    doThrow(cause).when(notComplete).get();
    helper.run();

    final InOrder order =
      inOrder(future, c.completable, complete, notComplete, completeStage, notCompleteStage, next);

    /* completed */
    order.verify(complete).get();
    order.verify(future).whenCancelled(any());
    order.verify(completeStage).thenApply(any());
    order.verify(next).handle(future);

    order.verify(complete).get();
    order.verify(notComplete).get();
    order.verify(notCompleteStage).whenDone(any());

    verifyNotComplete(order, notComplete, notCompleteStage, next);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithComplete() {
    final Supplier<Stage<Void>> complete = Mockito.mock(Supplier.class);
    final Stage<Void> completeStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.WithComplete helper =
      c.completable.new WithComplete(future, complete);

    doReturn(completeStage).when(complete).get();

    doReturn(next).when(completeStage).thenApply(any());

    c.complete(result);
    helper.run();

    doThrow(cause).when(complete).get();
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(future, c.completable, complete, completeStage, next);

    /* completed */
    order.verify(complete).get();
    order.verify(future).whenCancelled(any());
    order.verify(completeStage).thenApply(any());
    order.verify(next).handle(future);

    order.verify(complete).get();
    order.verify(future).fail(cause);

    /* cancelled */
    order.verify(future).cancel();

    /* failed */
    order.verify(future).fail(any());

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithNotComplete() {
    final Supplier<Stage<Void>> notComplete = Mockito.mock(Supplier.class);
    final Stage<Void> notCompleteStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.WithNotComplete helper =
      c.completable.new WithNotComplete(future, notComplete);

    doReturn(notCompleteStage).when(notComplete).get();

    doReturn(next).when(notCompleteStage).thenCancel();
    doReturn(next).when(notCompleteStage).thenFail(any());

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    doThrow(cause).when(notComplete).get();
    helper.run();

    doReturn(notCompleteStage).when(notComplete).get();
    c.fail(cause);
    helper.run();

    doThrow(cause).when(notComplete).get();
    helper.run();

    final InOrder order = inOrder(future, c.completable, notComplete, notCompleteStage, next);

    /* completed */
    order.verify(future).complete(result);

    verifyNotComplete(order, notComplete, notCompleteStage, next);

    order.verifyNoMoreInteractions();
  }

  private void verifyNotComplete(
    final InOrder order, final Supplier<Stage<Void>> notComplete,
    final Stage<Void> notCompleteStage, final Stage<From> next
  ) {
    /* cancelled */
    order.verify(notComplete).get();
    order.verify(future).whenCancelled(any());
    order.verify(notCompleteStage).thenCancel();
    order.verify(next).handle(future);

    order.verify(notComplete).get();
    order.verify(future).fail(any());

    /* failed */
    order.verify(notComplete).get();
    order.verify(future).whenCancelled(any());
    order.verify(notCompleteStage).thenFail(any());
    order.verify(next).handle(future);

    order.verify(notComplete).get();
    order.verify(future).fail(any());
  }

  class Completable {
    final ConcurrentCompletable<ConcurrentCompletableTest.From> completable =
      Mockito.spy(new ConcurrentCompletable<>(caller));

    public void complete(final From result) {
      completable.state.set(ConcurrentCompletable.COMPLETED);
      completable.result = result;
    }

    public void cancel() {
      completable.state.set(ConcurrentCompletable.CANCELLED);
      completable.result = ConcurrentCompletable.CANCEL;
    }

    public void fail(final Exception cause) {
      completable.state.set(ConcurrentCompletable.FAILED);
      completable.result = cause;
    }
  }

  private interface To {
  }

  private interface From {
  }
}
