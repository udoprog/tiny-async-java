package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;

public abstract class TransformHelperTestBase<T> {
  private Function<T, CompletionStage<Object>> transform;
  private CompletableFuture<Object> target;
  private CompletionHandle<Object> done;

  private RuntimeException e;
  private T from;

  private final Object onResult = new Object();
  private final Exception onCause = new Exception();

  private int cancelledTimes;
  private int resolvedTimes;
  private int failedTimes;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    transform = mock(Function.class);
    target = mock(CompletableFuture.class);
    done = setupDone(transform, target);
    e = setupError();
    from = setupFrom();
    cancelledTimes = setupCancelled();
    resolvedTimes = setupResolved();
    failedTimes = setupFailed();
  }

  protected abstract CompletionHandle<Object> setupDone(
      Function<T, CompletionStage<Object>> transform, CompletableFuture<Object> target
  );

  protected RuntimeException setupError() {
    return new RuntimeException();
  }

  protected int setupFailed() {
    return 0;
  }

  protected int setupResolved() {
    return 0;
  }

  protected int setupCancelled() {
    return 0;
  }

  protected T setupFrom() {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFailed() throws Exception {
    final CompletionStage<Object> f = mock(CompletionStage.class);

    if (failedTimes == 1) {
      setupVerifyFutureDone(f);
    }

    doReturn(f).when(transform).apply(from);
    done.failed(e);
    verify(target).fail(any(Exception.class));
    verify(f, times(failedTimes)).handle(any(CompletionHandle.class));
  }

  @Test
  public void testResolvedThrows() throws Exception {
    doThrow(e).when(transform).apply(from);
    done.resolved(from);
    verify(target, times(Math.max(cancelledTimes, failedTimes))).complete(from);
    verify(target, times(resolvedTimes)).fail(any(Exception.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testResolved() throws Exception {
    final CompletionStage<Object> f = mock(CompletionStage.class);

    if (resolvedTimes == 1) {
      setupVerifyFutureDone(f);
    }

    doReturn(f).when(transform).apply(from);
    done.resolved(from);
    verify(target, times(Math.max(cancelledTimes, failedTimes))).complete(from);
    verify(f, times(resolvedTimes)).handle(any(CompletionHandle.class));
  }

  @Test
  public void testCancelledThrows() throws Exception {
    doThrow(e).when(transform).apply(from);
    done.cancelled();
    verify(target, times(Math.max(resolvedTimes, failedTimes))).cancel();
    verify(target, times(cancelledTimes)).fail(any(Exception.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCancelled() throws Exception {
    final CompletionStage<Object> f = mock(CompletionStage.class);

    if (cancelledTimes == 1) {
      setupVerifyFutureDone(f);
    }

    doReturn(f).when(transform).apply(from);
    done.cancelled();
    verify(target, times(1)).cancel();
    verify(f, times(cancelledTimes)).handle(any(CompletionHandle.class));
  }

  @SuppressWarnings("unchecked")
  private void setupVerifyFutureDone(final CompletionStage<Object> f) {
    doAnswer(invocation -> {
      final CompletionHandle<Object> done1 =
          (CompletionHandle<Object>) invocation.getArguments()[0];
      done1.cancelled();
      done1.resolved(onResult);
      done1.failed(onCause);

      verify(target).cancel();
      verify(target).complete(onResult);
      verify(target).fail(onCause);
      return null;
    }).when(f).handle(any(CompletionHandle.class));
  }
}
