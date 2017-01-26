package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamCollectHelperTest {
  @Mock
  public Caller caller;

  @Mock
  public Consumer<Object> consumer;

  @Mock
  public Supplier<Object> supplier;

  @Mock
  public Completable<Object> target;

  private final Object transformed = new Object();
  private final Object result = new Object();
  private final RuntimeException e = new RuntimeException();

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(caller).execute(any(Runnable.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroSize() {
    new StreamCollectHelper<>(caller, 0, consumer, supplier, target);
  }

  @Test
  public void testOneFailed() throws Exception {
    final StreamCollectHelper<Object, Object> helper =
        new StreamCollectHelper<>(caller, 2, consumer, supplier, target);

    when(supplier.get()).thenReturn(transformed);

    helper.completed(result);
    verify(caller).execute(any(Runnable.class));
    verify(target, never()).complete(transformed);

    helper.failed(e);
    verify(supplier, never()).get();
    verify(target).fail(e);
  }

  @Test
  public void testOneCancelled() throws Exception {
    final StreamCollectHelper<Object, Object> helper =
        new StreamCollectHelper<>(caller, 2, consumer, supplier, target);

    when(supplier.get()).thenReturn(transformed);

    helper.completed(result);
    verify(caller).execute(any(Runnable.class));
    verify(target, never()).complete(transformed);

    helper.cancelled();
    verify(supplier, never()).get();
    verify(target).cancel();
  }

  @Test
  public void testAllResolved() throws Exception {
    final StreamCollectHelper<Object, Object> helper =
        new StreamCollectHelper<>(caller, 2, consumer, supplier, target);

    when(supplier.get()).thenReturn(transformed);

    helper.completed(result);
    verify(caller).execute(any(Runnable.class));
    verify(target, never()).complete(transformed);

    helper.completed(result);
    verify(supplier).get();
    verify(target).complete(transformed);
  }

  @Test
  public void testEndThrows() throws Exception {
    final StreamCollectHelper<Object, Object> helper =
        new StreamCollectHelper<Object, Object>(caller, 1, consumer, supplier, target);

    when(supplier.get()).thenThrow(e);

    helper.completed(result);
    verify(target).fail(any(Exception.class));
  }
}
