package eu.toolchain.concurrent;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

@RunWith(MockitoJUnitRunner.class)
public abstract class ImmediateAsyncFutureTestBase {
  @Mock
  private Throwable cause;
  @Mock
  private From result;
  @Mock
  private To to;
  @Mock
  private Stage<To> toFuture;
  @Mock
  private Caller caller;
  @Mock
  private Handle<From> done;
  @Mock
  private Consumer<From> resolved;
  @Mock
  private Runnable finished;
  @Mock
  private Consumer<Throwable> failed;
  @Mock
  private Runnable cancelled;
  @Mock
  private Stage<?> other;

  private AbstractImmediate<From> underTest;

  private ExpectedState expected;

  protected abstract AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  );

  protected abstract ExpectedState setupState();

  @Rule
  public ExpectedException except = ExpectedException.none();

  @Before
  public void setup() {
    underTest = spy(setupFuture(caller, result, cause));
    expected = setupState();

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(caller).execute(any(Runnable.class));
  }

  @Test
  public void testIsDone() throws Exception {
    assertTrue(underTest.isDone());
  }

  @Test
  public void testIsResolved() throws Exception {
    assertEquals(isResolved(), underTest.isCompleted());
  }

  @Test
  public void testIsFailed() throws Exception {
    assertEquals(isFailed(), underTest.isFailed());
  }

  @Test
  public void testIsCancelled() throws Exception {
    assertEquals(isCancelled(), underTest.isCancelled());
  }

  @Test
  public void testCause() throws Exception {
    if (!isFailed()) {
      except.expect(IllegalStateException.class);
    }

    assertNotNull(underTest.cause());
  }

  @Test
  public void testGet() throws Exception {
    if (isCancelled()) {
      except.expect(CancellationException.class);
    }

    if (isFailed()) {
      except.expect(ExecutionException.class);
    }

    underTest.join();
  }

  @Test
  public void testGetWithTimeout() throws Exception {
    if (isCancelled()) {
      except.expect(CancellationException.class);
    }

    if (isFailed()) {
      except.expect(ExecutionException.class);
    }

    underTest.join(1, TimeUnit.SECONDS);
  }

  @Test
  public void testGetNow() throws Exception {
    if (isCancelled()) {
      except.expect(CancellationException.class);
    }

    if (isFailed()) {
      except.expect(ExecutionException.class);
    }

    underTest.joinNow();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenApply() throws Exception {
    final Function<From, To> fn = mock(Function.class);
    doReturn(to).when(fn).apply(result);
    assertThat(underTest.thenApply(fn), is(expected()));
    verify(fn, completed()).apply(result);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenCompose() throws Exception {
    final Function<From, Stage<To>> fn = mock(Function.class);

    doReturn(toFuture).when(fn).apply(result);
    assertThat(underTest.thenCompose(fn), is(expected(toFuture)));
    verify(fn, completed()).apply(result);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenCatchFailed() throws Exception {
    final Function<Throwable, From> transform = mock(Function.class);

    doReturn(underTest).when(underTest).immediateCatchFailed(transform, cause);
    assertEquals(underTest, underTest.thenApplyFailed(transform));
    verify(underTest, failed()).immediateCatchFailed(transform, cause);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenComposeFailed() throws Exception {
    final Function<Throwable, Stage<From>> transform = mock(Function.class);

    doReturn(underTest).when(underTest).immediateComposeFailed(transform, cause);
    assertEquals(underTest, underTest.thenComposeFailed(transform));
    verify(underTest, failed()).immediateComposeFailed(transform, cause);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenCatchCancelled() throws Exception {
    final Supplier<From> transform = mock(Supplier.class);

    doReturn(underTest).when(underTest).immediateCatchCancelled(transform);
    assertEquals(underTest, underTest.thenApplyCancelled(transform));
    verify(underTest, cancelled()).immediateCatchCancelled(transform);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void thenComposeCancelled() throws Exception {
    final Supplier<Stage<From>> transform = mock(Supplier.class);

    doReturn(underTest).when(underTest).immediateComposeCancelled(transform);
    assertEquals(underTest, underTest.thenComposeCancelled(transform));
    verify(underTest, cancelled()).immediateComposeCancelled(transform);
  }

  private boolean isResolved() {
    return expected == ExpectedState.COMPLETED;
  }

  private boolean isCancelled() {
    return expected == ExpectedState.CANCELLED;
  }

  private boolean isFailed() {
    return expected == ExpectedState.FAILED;
  }

  private VerificationMode completed() {
    if (expected == ExpectedState.COMPLETED) {
      return times(1);
    }

    return never();
  }

  private VerificationMode cancelled() {
    if (expected == ExpectedState.CANCELLED) {
      return times(1);
    }

    return never();
  }

  private VerificationMode failed() {
    if (expected == ExpectedState.FAILED) {
      return times(1);
    }

    return never();
  }

  private Stage<To> expected() {
    switch (expected) {
      case CANCELLED:
        return new ImmediateCancelled<>(caller);
      case COMPLETED:
        return new ImmediateCompleted<>(caller, to);
      case FAILED:
        return new ImmediateFailed<>(caller, cause);
      default:
        throw new IllegalStateException("Unexpected mode: " + expected);
    }
  }

  private Stage<To> expected(final Stage<To> completed) {
    switch (expected) {
      case CANCELLED:
        return new ImmediateCancelled<>(caller);
      case COMPLETED:
        return completed;
      case FAILED:
        return new ImmediateFailed<>(caller, cause);
      default:
        throw new IllegalStateException("Unexpected mode: " + expected);
    }
  }

  protected interface From {
  }

  protected interface To {
  }

  protected enum ExpectedState {
    COMPLETED, CANCELLED, FAILED
  }
}
