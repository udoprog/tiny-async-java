package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
  private FutureFramework async;
  @Mock
  private FutureCaller caller;
  @Mock
  private CompletionHandle<From> done;
  @Mock
  private Consumer<From> resolved;
  @Mock
  private Runnable finished;
  @Mock
  private Consumer<Throwable> failed;
  @Mock
  private Runnable cancelled;
  @Mock
  private CompletionStage<?> other;

  private AbstractImmediateCompletionStage<From> underTest;

  private ExpectedState expected;

  protected abstract AbstractImmediateCompletionStage<From> setupFuture(
      FutureFramework async, FutureCaller caller, From result, Throwable cause
  );

  protected abstract ExpectedState setupState();

  @Rule
  public ExpectedException except = ExpectedException.none();

  @Before
  public void setup() {
    underTest = spy(setupFuture(async, caller, result, cause));
    expected = setupState();
  }

  @Test
  public void testBind() throws Exception {
    underTest.bind(other);
    verify(other, cancelled()).cancel();
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
  public void testTransform() throws Exception {
    final Function<From, To> transform = mock(Function.class);
    final CompletionStage<To> target = mock(CompletionStage.class);

    doReturn(target).when(underTest).transformResolved(transform, result);
    doReturn(target).when(async).cancelled();
    doReturn(target).when(async).failed(any(Exception.class));

    assertEquals(target, underTest.thenApply(transform));

    verify(underTest, resolved()).transformResolved(transform, result);
    verify(async, cancelled()).cancelled();
    verify(async, failed()).failed(any(Exception.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLazyTransform() throws Exception {
    final Function<From, CompletionStage<To>> transform = mock(Function.class);
    final CompletionStage<To> target = mock(CompletionStage.class);

    doReturn(target).when(underTest).lazyTransformResolved(transform, result);
    doReturn(target).when(async).cancelled();
    doReturn(target).when(async).failed(any(Exception.class));

    assertEquals(target, underTest.thenCompose(transform));

    verify(underTest, resolved()).lazyTransformResolved(transform, result);
    verify(async, cancelled()).cancelled();
    verify(async, failed()).failed(any(Exception.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTransformFailed() throws Exception {
    final Function<Throwable, From> transform = mock(Function.class);

    doReturn(underTest).when(underTest).transformFailed(transform, cause);

    assertEquals(underTest, underTest.thenCatchFailed(transform));

    verify(underTest, failed()).transformFailed(transform, cause);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLazyTransformFailed() throws Exception {
    final Function<Throwable, CompletionStage<From>> transform = mock(Function.class);

    doReturn(underTest).when(underTest).lazyTransformFailed(transform, cause);

    assertEquals(underTest, underTest.thenComposeFailed(transform));

    verify(underTest, failed()).lazyTransformFailed(transform, cause);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTransformCancelled() throws Exception {
    final Supplier<From> transform = mock(Supplier.class);

    doReturn(underTest).when(underTest).transformCancelled(transform);
    assertEquals(underTest, underTest.thenCatchCancelled(transform));
    verify(underTest, cancelled()).transformCancelled(transform);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLazyTransformCancelled() throws Exception {
    final Supplier<CompletionStage<From>> transform = mock(Supplier.class);

    doReturn(underTest).when(underTest).lazyTransformCancelled(transform);
    assertEquals(underTest, underTest.thenComposeCancelled(transform));
    verify(underTest, cancelled()).lazyTransformCancelled(transform);
  }

  private boolean isResolved() {
    return expected == ExpectedState.RESOLVED;
  }

  private boolean isCancelled() {
    return expected == ExpectedState.CANCELLED;
  }

  private boolean isFailed() {
    return expected == ExpectedState.FAILED;
  }

  private VerificationMode resolved() {
    if (expected == ExpectedState.RESOLVED) {
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

  protected static interface From {
  }

  protected static interface To {
  }

  protected static enum ExpectedState {
    RESOLVED, CANCELLED, FAILED
  }
}
