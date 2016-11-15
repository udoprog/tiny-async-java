package eu.toolchain.concurrent;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractCompletableIT {
  public static final int VALUE = 10;

  private final RuntimeException cause = new RuntimeException();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Rule
  public Timeout timeout = Timeout.millis(500);

  @Mock
  public FutureCaller caller;

  private Completable<Integer> outer;
  private Completable<Integer> inner;

  @Before
  public void setUp() {
    outer = setupFuture();
    inner = setupFuture();

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(caller).execute(any(Runnable.class));
  }

  protected abstract <T> Completable<T> setupFuture(FutureCaller caller);

  private <T> Completable<T> setupFuture() {
    return setupFuture(caller);
  }

  /* thenApply */

  @Test
  public void thenApply() throws Exception {
    final Completable<Integer> a = setupFuture();
    final Stage<Integer> next = a.thenApply(v -> v + VALUE);

    a.complete(VALUE);
    assertThat(next.join(), is(VALUE + VALUE));
  }

  @Test
  public void thenApplyFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenApply(v -> v + VALUE);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    outer.fail(cause);
    next.join();
  }

  @Test
  public void thenApplyCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenApply(v -> v + VALUE);

    expected.expect(CancellationException.class);
    outer.cancel();
    next.join();
  }

  @Test
  public void thenApplyCancelNext() throws Exception {
    final Stage<Integer> next = outer.thenApply(v -> v + VALUE);

    expected.expect(CancellationException.class);
    next.cancel();
    outer.join();
  }

  /* thenCompose */

  @Test
  public void thenCompose() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    inner.complete(VALUE);
    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    inner.complete(VALUE);
    outer.fail(cause);
    next.join();
  }

  @Test
  public void thenComposeFailInner() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    inner.fail(cause);
    outer.complete(VALUE);
    next.join();
  }

  @Test
  public void thenComposeCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    expected.expect(CancellationException.class);
    inner.complete(VALUE);
    outer.cancel();
    next.join();
  }

  @Test
  public void thenComposeCancelNext() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    expected.expect(CancellationException.class);
    inner.complete(VALUE);
    next.cancel();
    next.join();
  }

  @Test
  public void thenComposeCancelInner() throws Exception {
    final Stage<Integer> next = outer.thenCompose(v -> inner);

    expected.expect(CancellationException.class);
    inner.cancel();
    outer.complete(VALUE);
    next.join();
  }
  
  /* thenApplyCancelled */

  @Test
  public void thenApplyCancelled() throws Exception {
    final Stage<Integer> next = outer.thenApplyCancelled(() -> VALUE);

    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenApplyCancelledFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenApplyCancelled(() -> VALUE);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    outer.fail(cause);
    next.join();
  }

  @Test
  public void thenApplyCancelledCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenApplyCancelled(() -> VALUE);

    outer.cancel();
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenApplyCancelledCancelNext() throws Exception {
    final Stage<Integer> next = outer.thenApplyCancelled(() -> VALUE);

    expected.expect(CancellationException.class);
    next.cancel();
    outer.join();
  }

  /* thenComposeCancelled */

  @Test
  public void thenComposeCancelled() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    inner.complete(VALUE);
    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeCancelledFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    inner.complete(VALUE);
    outer.fail(cause);
    next.join();
  }

  @Test
  public void thenComposeCancelledFailInner() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    expected.expect(ExecutionException.class);
    expected.expectCause(is(cause));
    inner.fail(cause);
    outer.cancel();
    next.join();
  }

  @Test
  public void thenComposeCancelledCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    inner.complete(VALUE);
    outer.cancel();
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeCancelledCancelNext() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    expected.expect(CancellationException.class);
    inner.complete(VALUE);
    next.cancel();
    next.join();
  }

  @Test
  public void thenComposeCancelledCancelInner() throws Exception {
    final Stage<Integer> next = outer.thenComposeCancelled(() -> inner);

    expected.expect(CancellationException.class);
    inner.cancel();
    outer.cancel();
    next.join();
  }
  
  /* thenApplyFailed */

  @Test
  public void thenApplyFailed() throws Exception {
    final Stage<Integer> next = outer.thenApplyFailed(t -> VALUE);

    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenApplyFailedFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenApplyFailed(t -> VALUE);

    outer.fail(cause);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenApplyFailedCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenApplyFailed(t -> VALUE);

    expected.expect(CancellationException.class);
    outer.cancel();
    next.join();
  }

  @Test
  public void thenApplyFailedCancelNext() throws Exception {
    expected.expect(CancellationException.class);

    final Stage<Integer> next = outer.thenApplyFailed(t -> VALUE);

    next.cancel();
    outer.join();
  }

  /* thenComposeFailed */

  @Test
  public void thenComposeFailed() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    inner.complete(VALUE);
    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeFailedFailOuter() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    inner.complete(VALUE);
    outer.fail(cause);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeFailedFailInner() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    inner.fail(cause);
    outer.complete(VALUE);
    assertThat(next.join(), is(VALUE));
  }

  @Test
  public void thenComposeFailedCancelOuter() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    expected.expect(CancellationException.class);
    inner.complete(VALUE);
    outer.cancel();
    next.join();
  }

  @Test
  public void thenComposeFailedCancelNext() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    expected.expect(CancellationException.class);
    inner.complete(VALUE);
    next.cancel();
    next.join();
  }

  @Test
  public void thenComposeFailedCancelInner() throws Exception {
    final Stage<Integer> next = outer.thenComposeFailed(t -> inner);

    expected.expect(CancellationException.class);
    inner.cancel();
    outer.cancel();
    next.join();
  }
}
