package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Test;

public class DelayedCollectCoordinatorTest {
  private FutureCaller caller;
  private StreamCollector<Object, Object> collector;
  private CompletableFuture<Object> future;
  private Callable<CompletionStage<Object>> callable;
  private Callable<CompletionStage<Object>> callable2;
  private Callable<CompletionStage<Object>> callable3;
  private Callable<CompletionStage<Object>> callable4;
  private CompletionStage<Object> f;
  private CompletionStage<Object> f2;
  private CompletionStage<Object> f3;
  private CompletionStage<Object> f4;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    caller = mock(FutureCaller.class);
    collector = mock(StreamCollector.class);
    future = mock(CompletableFuture.class);
    callable = mock(Callable.class);
    callable2 = mock(Callable.class);
    callable3 = mock(Callable.class);
    callable4 = mock(Callable.class);

    f = mock(CompletionStage.class);
    f2 = mock(CompletionStage.class);
    f3 = mock(CompletionStage.class);
    f4 = mock(CompletionStage.class);

    when(callable.call()).thenReturn(f);
    when(callable2.call()).thenReturn(f2);
    when(callable3.call()).thenReturn(f3);
    when(callable4.call()).thenReturn(f4);

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(caller).execute(any(Runnable.class));
  }

  @Test
  public void testCallFutureDoneMethods() throws Exception {
    final List<Callable<CompletionStage<Object>>> callables = ImmutableList.of();

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, future, 1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.cancelled();
    coordinator.completed(result);
    coordinator.failed(cause);

    verify(collector).cancelled();
    verify(collector).completed(result);
    verify(collector).failed(cause);
  }

  @Test
  public void testBasic() throws Exception {
    final List<Callable<CompletionStage<Object>>> callables = ImmutableList.of(callable, callable2);

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, future, 1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.run();

    coordinator.completed(result);
    verify(collector, never()).end(2, 0, 0);

    coordinator.completed(result);
    verify(collector).end(2, 0, 0);

    verify(collector, never()).cancelled();
    verify(collector, times(2)).completed(result);
    verify(collector, never()).failed(cause);
  }

  @Test
  public void testCancel() throws Exception {
    final List<Callable<CompletionStage<Object>>> callables =
        ImmutableList.of(callable, callable2, callable3, callable4);

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, future, 1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.run();

    coordinator.completed(result);
    verify(collector, never()).end(any(Integer.class), any(Integer.class), any(Integer.class));

    coordinator.cancelled();
    verify(collector).end(1, 0, 3);

    verify(collector, times(3)).cancelled();
    verify(collector, times(1)).completed(result);
    verify(collector, never()).failed(cause);
  }
}
