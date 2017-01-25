package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ConcurrentCompletableTest {
  @Rule
  public ExpectedException expected = ExpectedException.none();

  private static final RuntimeException cause = new RuntimeException();
  private static final RuntimeException cause2 = new RuntimeException();

  @Mock
  private From result;
  @Mock
  private To to;
  @Mock
  private Caller caller;

  private Completable c;

  @Before
  public void setup() {
    c = new Completable();
  }

  /**
   * verify alias of #{link {@link eu.toolchain.concurrent.ConcurrentCompletable#complete(Object)}}
   */
  @Test
  public void testCompleted() {
    doReturn(true).when(c.completable).complete(result);
    c.completable.completed(result);
    verify(c.completable).complete(result);
  }

  /**
   * verify alias of #{link
   * {@link eu.toolchain.concurrent.ConcurrentCompletable#fail(java.lang.Throwable)}}
   */
  @Test
  public void testFailed() {
    doReturn(true).when(c.completable).fail(cause);
    c.completable.failed(cause);
    verify(c.completable).fail(cause);
  }

  /**
   * verify alias of #{link {@link eu.toolchain.concurrent.ConcurrentCompletable#cancel()}}
   */
  @Test
  public void testCancelled() {
    doReturn(true).when(c.completable).cancel();
    c.completable.cancelled();
    verify(c.completable).cancel();
  }

  @Test
  public void testComplete() {
    assertEquals(ConcurrentCompletable.PENDING, c.completable.state.get());
    assertTrue(c.completable.complete(result));
    assertEquals(ConcurrentCompletable.COMPLETED, c.completable.state.get());
    assertEquals(result, c.completable.result);

    verifyFinalized();
  }

  @Test
  public void testCompleteNull() {
    assertEquals(ConcurrentCompletable.PENDING, c.completable.state.get());
    assertTrue(c.completable.complete(null));
    assertEquals(ConcurrentCompletable.COMPLETED, c.completable.state.get());
    assertEquals(ConcurrentCompletable.NULL, c.completable.result);

    verifyFinalized();
  }

  @Test
  public void testFail() {
    assertEquals(ConcurrentCompletable.PENDING, c.completable.state.get());
    assertTrue(c.completable.fail(cause));
    assertEquals(ConcurrentCompletable.FAILED, c.completable.state.get());
    assertEquals(cause, c.completable.result);

    verifyFinalized();
  }

  @Test
  public void testFailNull() {
    expected.expect(NullPointerException.class);
    expected.expectMessage("cause");

    assertEquals(ConcurrentCompletable.PENDING, c.completable.state.get());
    assertTrue(c.completable.fail(null));
  }

  @Test
  public void testCancel() {
    assertEquals(ConcurrentCompletable.PENDING, c.completable.state.get());
    assertTrue(c.completable.cancel());
    assertEquals(ConcurrentCompletable.CANCELLED, c.completable.state.get());
    assertEquals(ConcurrentCompletable.CANCEL, c.completable.result);

    verifyFinalized();
  }

  @Test
  public void testWhenDone() {
    final Runnable runnable = mock(Runnable.class);

    doReturn(false).when(c.completable).add(runnable);
    c.completable.whenDone(runnable);

    doReturn(true).when(c.completable).add(runnable);
    c.completable.whenDone(runnable);

    final InOrder order = inOrder(c.completable, caller, runnable);
    order.verify(c.completable).add(runnable);
    order.verify(caller).execute(runnable);
    order.verify(c.completable).add(runnable);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWhenComplete() {
    final Stage<From> stage = mock(Stage.class);
    final Consumer<From> consumer = mock(Consumer.class);
    final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(stage).when(c.completable).whenDone(captor.capture());

    assertEquals(stage, c.completable.whenComplete(consumer));

    verify(c.completable).whenDone(captor.getValue());
  }

  @Test
  public void testWhenFailed() {
    final Stage<From> stage = mock(Stage.class);
    final Consumer<? super Throwable> consumer = mock(Consumer.class);
    final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(stage).when(c.completable).whenDone(captor.capture());

    assertEquals(stage, c.completable.whenFailed(consumer));

    verify(c.completable).whenDone(captor.getValue());
  }

  @Test
  public void testWhenCancelled() {
    final Stage<From> stage = mock(Stage.class);
    final Runnable runnable = mock(Runnable.class);
    final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(stage).when(c.completable).whenDone(captor.capture());

    assertEquals(stage, c.completable.whenCancelled(runnable));

    verify(c.completable).whenDone(captor.getValue());
  }

  /* verify that the state of a completable cannot be changed */
  private void verifyFinalized() {
    final int state = c.completable.state.get();
    final Object result = c.completable.result;

    assertFalse(c.completable.complete(this.result));
    assertEquals(state, c.completable.state.get());
    assertEquals(result, c.completable.result);
    assertFalse(c.completable.fail(cause));
    assertEquals(state, c.completable.state.get());
    assertEquals(result, c.completable.result);
    assertFalse(c.completable.cancel());
    assertEquals(state, c.completable.state.get());
    assertEquals(result, c.completable.result);
  }

  @Test
  public void testThenApplyRunnable() {
    final ConcurrentCompletable<To> target = Mockito.mock(ConcurrentCompletable.class);
    final Function<From, To> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenApplyRunnable helper =
      c.completable.new ThenApplyRunnable<To>(target, fn);

    doReturn(to).when(fn).apply(result);

    c.complete(result);
    helper.run();

    doThrow(cause).when(fn).apply(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, target);
    order.verify(fn).apply(result);
    order.verify(target).complete(to);
    order.verify(fn).apply(result);
    order.verify(target).fail(cause);
    order.verify(target).cancel();
    order.verify(target).fail(cause);
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenComposeRunnable() {
    final ConcurrentCompletable<To> target = Mockito.mock(ConcurrentCompletable.class);
    final Function<From, Stage<To>> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenComposeRunnable helper =
      c.completable.new ThenComposeRunnable<To>(target, fn);

    doNothing().when(c.completable).handleStage(any(), eq(target));

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, target, c.completable);

    order.verify(c.completable).handleStage(any(), eq(target));
    order.verify(target).cancel();
    order.verify(target).fail(cause);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenApplyFailedRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);
    final Function<Throwable, From> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenApplyFailedRunnable helper =
      c.completable.new ThenApplyFailedRunnable(target, fn);

    doReturn(result).when(fn).apply(cause);

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();
    doThrow(cause2).when(fn).apply(cause);
    helper.run();

    final InOrder order = inOrder(fn, target);

    order.verify(target).complete(result);
    order.verify(target).cancel();
    order.verify(fn).apply(cause);
    order.verify(target).fail(cause2);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenComposeFailedRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);
    final Function<Throwable, Stage<From>> fn = Mockito.mock(Function.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.ThenComposeFailedRunnable helper =
      c.completable.new ThenComposeFailedRunnable(target, fn);

    doNothing().when(c.completable).handleStage(any(), eq(target));
    doReturn(result).when(fn).apply(cause);

    c.complete(result);
    helper.run();

    c.cancel();
    helper.run();

    c.fail(cause);
    helper.run();

    final InOrder order = inOrder(fn, target, c.completable);

    order.verify(target).complete(result);
    order.verify(target).cancel();
    order.verify(c.completable).handleStage(any(), eq(target));

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithCloserRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);
    final Supplier<Stage<Void>> complete = Mockito.mock(Supplier.class);
    final Supplier<Stage<Void>> notComplete = Mockito.mock(Supplier.class);
    final Stage<Void> completeStage = Mockito.mock(Stage.class);
    final Stage<Void> notCompleteStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.WithCloserRunnable helper =
      c.completable.new WithCloserRunnable(target, complete, notComplete);

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
      inOrder(target, c.completable, complete, notComplete, completeStage, notCompleteStage, next);

    /* completed */
    order.verify(complete).get();
    order.verify(target).whenCancelled(any());
    order.verify(completeStage).thenApply(any());
    order.verify(next).handle(target);

    order.verify(complete).get();
    order.verify(notComplete).get();
    order.verify(notCompleteStage).whenDone(any());

    verifyNotCompleteRunnable(target, order, notComplete, notCompleteStage, next);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithCompleteRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);
    final Supplier<Stage<Void>> complete = Mockito.mock(Supplier.class);
    final Stage<Void> completeStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final Completable c = new Completable();

    final ConcurrentCompletable.WithCompleteRunnable helper =
      c.completable.new WithCompleteRunnable(target, complete);

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

    final InOrder order = inOrder(target, c.completable, complete, completeStage, next);

    /* completed */
    order.verify(complete).get();
    order.verify(target).whenCancelled(any());
    order.verify(completeStage).thenApply(any());
    order.verify(next).handle(target);

    order.verify(complete).get();
    order.verify(target).fail(cause);

    /* cancelled */
    order.verify(target).cancel();

    /* failed */
    order.verify(target).fail(any());

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testWithNotCompleteRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);
    final Supplier<Stage<Void>> notComplete = Mockito.mock(Supplier.class);
    final Stage<Void> notCompleteStage = Mockito.mock(Stage.class);
    final Stage<From> next = Mockito.mock(Stage.class);

    final ConcurrentCompletable.WithNotCompleteRunnable helper =
      c.completable.new WithNotCompleteRunnable(target, notComplete);

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

    final InOrder order = inOrder(target, c.completable, notComplete, notCompleteStage, next);

    /* completed */
    order.verify(target).complete(result);

    verifyNotCompleteRunnable(target, order, notComplete, notCompleteStage, next);

    order.verifyNoMoreInteractions();
  }

  @Test
  public void testThenFailRunnable() {
    final ConcurrentCompletable<From> target = Mockito.mock(ConcurrentCompletable.class);

    final ConcurrentCompletable.ThenFailRunnable helper =
      c.completable.new ThenFailRunnable<>(target, cause);

    c.fail(cause);
    helper.run();

    c.cancel();
    helper.run();

    c.complete(result);
    helper.run();

    final InOrder order = inOrder(target);

    order.verify(target, times(3)).fail(any(Exception.class));

    order.verifyNoMoreInteractions();
  }

  private void verifyNotCompleteRunnable(
    final ConcurrentCompletable<From> target, final InOrder order,
    final Supplier<Stage<Void>> notComplete, final Stage<Void> notCompleteStage,
    final Stage<From> next
  ) {
    /* cancelled */
    order.verify(notComplete).get();
    order.verify(target).whenCancelled(any());
    order.verify(notCompleteStage).thenCancel();
    order.verify(next).handle(target);

    order.verify(notComplete).get();
    order.verify(target).fail(any());

    /* failed */
    order.verify(notComplete).get();
    order.verify(target).whenCancelled(any());
    order.verify(notCompleteStage).thenFail(any());
    order.verify(next).handle(target);

    order.verify(notComplete).get();
    order.verify(target).fail(any());
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
