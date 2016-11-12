package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;

public class TransformHelperTest {
  private static final Object result = new Object();
  private static final Object transformed = new Object();

  private static final RuntimeException e = new RuntimeException();

  private static final RuntimeException cause = new RuntimeException();
  private static final RuntimeException transformedCause = new RuntimeException();

  private Function<Object, Object> transform;
  private Function<Throwable, Object> errorTransform;
  private Supplier<Object> cancelledTransform;

  private Function<Object, CompletionStage<Object>> lazyTransform;
  private Function<Throwable, CompletionStage<Object>> lazyErrorTransform;
  private Supplier<CompletionStage<Object>> lazyCancelledTransform;

  private CompletableFuture<Object> target;
  private CompletionStage<Object> f;

  private ThenApplyHelper<Object, Object> resolved;
  private ThenCatchFailedHelper<Object> failed;
  private ThenCatchCancelledHelper<Object> cancelled;

  private ThenComposeHelper<Object, Object> lazyResolved;
  private ThenComposeFailedHelper<Object> lazyFailed;
  private TheComposeCancelledHelper<Object> lazyCancelled;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    transform = mock(Function.class);
    errorTransform = mock(Function.class);
    cancelledTransform = mock(Supplier.class);

    lazyTransform = mock(Function.class);
    lazyErrorTransform = mock(Function.class);
    lazyCancelledTransform = mock(Supplier.class);

    target = mock(CompletableFuture.class);
    f = mock(CompletionStage.class);

    doAnswer(invocation -> {
      final CompletionHandle<Object> done = (CompletionHandle<Object>) invocation.getArguments()[0];

      done.resolved(transformed);
      done.failed(transformedCause);
      done.cancelled();

      return null;
    }).when(f).handle(any(CompletionHandle.class));

    when(transform.apply(result)).thenReturn(transformed);
    when(errorTransform.apply(cause)).thenReturn(transformed);
    when(cancelledTransform.get()).thenReturn(transformed);

    when(lazyTransform.apply(result)).thenReturn(f);
    when(lazyErrorTransform.apply(cause)).thenReturn(f);
    when(lazyCancelledTransform.get()).thenReturn(f);

    resolved = new ThenApplyHelper<>(transform, target);
    failed = new ThenCatchFailedHelper<>(errorTransform, target);
    cancelled = new ThenCatchCancelledHelper<>(cancelledTransform, target);

    lazyResolved = new ThenComposeHelper<>(lazyTransform, target);
    lazyFailed = new ThenComposeFailedHelper<>(lazyErrorTransform, target);
    lazyCancelled = new TheComposeCancelledHelper<>(lazyCancelledTransform, target);
  }

  private void verifyTransform(int resolved, int failed, int cancelled) throws Exception {
    verify(transform, times(resolved)).apply(result);
    verify(errorTransform, times(failed)).apply(cause);
    verify(cancelledTransform, times(cancelled)).get();
  }

  @Test
  public void testResolved() throws Exception {
    resolved.resolved(result);
    failed.resolved(result);
    cancelled.resolved(result);

    verifyTransform(1, 0, 0);
    verify(target, times(1)).complete(transformed);
    verify(target, times(2)).complete(result);
  }

  @Test
  public void testResolvedThrows() throws Exception {
    when(transform.apply(result)).thenThrow(e);
    resolved.resolved(result);

    verifyTransform(1, 0, 0);
    verify(target, times(1)).fail(any(Exception.class));
  }

  @Test
  public void testFailed() throws Exception {
    resolved.failed(cause);
    failed.failed(cause);
    cancelled.failed(cause);

    verifyTransform(0, 1, 0);
    verify(target, times(1)).complete(transformed);
    verify(target, times(2)).fail(cause);
  }

  @Test
  public void testFailedThrows() throws Exception {
    when(errorTransform.apply(cause)).thenThrow(e);
    failed.failed(cause);

    verifyTransform(0, 1, 0);
    verify(target, times(1)).fail(any(Exception.class));
  }

  @Test
  public void testCancelled() throws Exception {
    resolved.cancelled();
    failed.cancelled();
    cancelled.cancelled();

    verifyTransform(0, 0, 1);
    verify(target, times(1)).complete(transformed);
    verify(target, times(2)).cancel();
  }

  @Test
  public void testCancelledThrows() throws Exception {
    when(cancelledTransform.get()).thenThrow(e);
    cancelled.cancelled();

    verifyTransform(0, 0, 1);
    verify(target, times(1)).fail(any(Exception.class));
  }

  private void verifyLazyTransform(int resolved, int failed, int cancelled) throws Exception {
    verify(lazyTransform, times(resolved)).apply(result);
    verify(lazyErrorTransform, times(failed)).apply(cause);
    verify(lazyCancelledTransform, times(cancelled)).get();
  }

  @Test
  public void testLazyResolved() throws Exception {
    lazyResolved.resolved(result);
    lazyFailed.resolved(result);
    lazyCancelled.resolved(result);

    verifyLazyTransform(1, 0, 0);

    verify(target, times(1)).complete(transformed);
    verify(target, times(1)).fail(transformedCause);
    verify(target, times(2)).complete(result);
    verify(target, times(1)).cancel();
  }

  @Test
  public void testLazyResolvedThrows() throws Exception {
    when(lazyTransform.apply(result)).thenThrow(e);
    lazyResolved.resolved(result);

    verifyLazyTransform(1, 0, 0);
    verify(target, times(1)).fail(any(Exception.class));
  }

  @Test
  public void testLazyFailed() throws Exception {
    lazyResolved.failed(cause);
    lazyFailed.failed(cause);
    lazyCancelled.failed(cause);

    verifyLazyTransform(0, 1, 0);

    verify(target, times(1)).complete(transformed);
    verify(target, times(1)).fail(transformedCause);
    verify(target, times(2)).fail(cause);
    verify(target, times(1)).cancel();
  }

  @Test
  public void testLazyFailedThrows() throws Exception {
    when(lazyErrorTransform.apply(cause)).thenThrow(e);
    lazyFailed.failed(cause);

    verifyLazyTransform(0, 1, 0);
    verify(target, times(1)).fail(any(Exception.class));
  }

  @Test
  public void testLazyCancelled() throws Exception {
    lazyResolved.cancelled();
    lazyFailed.cancelled();
    lazyCancelled.cancelled();

    verifyLazyTransform(0, 0, 1);

    verify(target, times(1)).complete(transformed);
    verify(target, times(1)).fail(transformedCause);
    verify(target, times(3)).cancel();
  }

  @Test
  public void testLazyCancelledThrows() throws Exception {
    when(lazyCancelledTransform.get()).thenThrow(e);
    lazyCancelled.cancelled();

    verifyLazyTransform(0, 0, 1);
    verify(target, times(1)).fail(any(Exception.class));
  }
}
