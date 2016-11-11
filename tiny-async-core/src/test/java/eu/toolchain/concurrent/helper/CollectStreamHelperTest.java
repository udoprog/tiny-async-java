package eu.toolchain.concurrent.helper;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.toolchain.concurrent.CollectStreamHelper;
import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.StreamCollector;
import org.junit.Before;
import org.junit.Test;

public class CollectStreamHelperTest {
  private FutureCaller caller;
  private StreamCollector<Object, Object> collector;
  private CompletableFuture<Object> target;

  private final Object transformed = new Object();
  private final Object result = new Object();
  private final Exception e = new Exception();

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    caller = mock(FutureCaller.class);
    collector = mock(StreamCollector.class);
    target = mock(CompletableFuture.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroSize() {
    new CollectStreamHelper<Object, Object>(caller, 0, collector, target);
  }

  @Test
  public void testOneFailed() throws Exception {
    final CollectStreamHelper<Object, Object> helper =
        new CollectStreamHelper<Object, Object>(caller, 2, collector, target);

    when(collector.end(1, 1, 0)).thenReturn(transformed);

    helper.resolved(result);
    verify(caller).resolve(collector, result);
    verify(target, never()).complete(transformed);

    helper.failed(e);
    verify(collector).end(1, 1, 0);
    verify(caller).fail(collector, e);
    verify(target).complete(transformed);
  }

  @Test
  public void testOneCancelled() throws Exception {
    final CollectStreamHelper<Object, Object> helper =
        new CollectStreamHelper<Object, Object>(caller, 2, collector, target);

    when(collector.end(1, 0, 1)).thenReturn(transformed);

    helper.resolved(result);
    verify(caller).resolve(collector, result);
    verify(target, never()).complete(transformed);

    helper.cancelled();
    verify(collector).end(1, 0, 1);
    verify(caller).cancel(collector);
    verify(target).complete(transformed);
  }

  @Test
  public void testAllResolved() throws Exception {
    final CollectStreamHelper<Object, Object> helper =
        new CollectStreamHelper<Object, Object>(caller, 2, collector, target);

    when(collector.end(2, 0, 0)).thenReturn(transformed);

    helper.resolved(result);
    verify(caller).resolve(collector, result);
    verify(target, never()).complete(transformed);

    helper.resolved(result);
    verify(collector).end(2, 0, 0);
    verify(caller, times(2)).resolve(collector, result);
    verify(target).complete(transformed);
  }

  @Test
  public void testEndThrows() throws Exception {
    final CollectStreamHelper<Object, Object> helper =
        new CollectStreamHelper<Object, Object>(caller, 1, collector, target);

    when(collector.end(1, 0, 0)).thenThrow(e);

    helper.resolved(result);
    verify(caller).resolve(collector, result);
    verify(target).fail(any(Exception.class));
  }
}
