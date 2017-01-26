package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DelayedCollectCoordinatorTest {
  @Mock
  private Caller caller;
  @Mock
  private Consumer<Object> consumer;
  @Mock
  private Supplier<Object> supplier;
  @Mock
  private Completable<Object> future;
  @Mock
  private Callable<Stage<Object>> callable;
  @Mock
  private Callable<Stage<Object>> callable2;
  @Mock
  private Callable<Stage<Object>> callable3;
  @Mock
  private Callable<Stage<Object>> callable4;
  @Mock
  private Stage<Object> f;
  @Mock
  private Stage<Object> f2;
  @Mock
  private Stage<Object> f3;
  @Mock
  private Stage<Object> f4;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
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
    final List<Callable<Stage<Object>>> callables = ImmutableList.of();

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, consumer, supplier, future,
            1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.cancelled();
    coordinator.completed(result);
    coordinator.failed(cause);

    verify(consumer).accept(result);
  }

  @Test
  public void testBasic() throws Exception {
    final List<Callable<Stage<Object>>> callables = ImmutableList.of(callable, callable2);

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, consumer, supplier, future,
            1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.run();

    coordinator.completed(result);
    verify(supplier, never()).get();

    coordinator.completed(result);
    verify(supplier).get();

    verify(consumer, times(2)).accept(result);
  }

  @Test
  public void testCancel() throws Exception {
    final List<Callable<Stage<Object>>> callables =
        ImmutableList.of(callable, callable2, callable3, callable4);

    final DelayedCollectCoordinator<Object, Object> coordinator =
        new DelayedCollectCoordinator<Object, Object>(caller, callables, consumer, supplier, future,
            1);

    final Object result = new Object();
    final Throwable cause = new Throwable();

    coordinator.run();

    coordinator.completed(result);
    verify(supplier, never()).get();

    coordinator.cancelled();
    verify(supplier).get();

    verify(consumer, times(1)).accept(result);
  }
}
