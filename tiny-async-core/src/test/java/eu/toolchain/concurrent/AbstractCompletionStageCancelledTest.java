package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractCompletionStageCancelledTest {
  private static final RuntimeException cause = new RuntimeException();

  private AbstractImmediateCompletionStage<To> base;

  @Mock
  private FutureFramework async;
  @Mock
  private Supplier<To> transform;
  @Mock
  private Supplier<CompletionStage<To>> lazyTransform;
  @Mock
  private To to;
  @Mock
  private CompletionStage<To> resolved;
  @Mock
  private CompletionStage<To> failed;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    base = mock(AbstractImmediateCompletionStage.class, Mockito.CALLS_REAL_METHODS);
    base.async = async;
  }

  @Test
  public void testTransformResolved() throws Exception {
    doReturn(to).when(transform).get();
    doReturn(resolved).when(async).completed(to);
    doReturn(failed).when(async).failed(any(Exception.class));

    assertEquals(resolved, base.transformCancelled(transform));

    final InOrder order = inOrder(transform, async);
    order.verify(transform).get();
    order.verify(async).completed(to);
    order.verify(async, never()).failed(any(Exception.class));
  }

  @Test
  public void testTransformResolvedThrows() throws Exception {
    doThrow(cause).when(transform).get();
    doReturn(resolved).when(async).completed(to);
    doReturn(failed).when(async).failed(any(Exception.class));

    assertEquals(failed, base.transformCancelled(transform));

    final InOrder order = inOrder(transform, async);
    order.verify(transform).get();
    order.verify(async, never()).completed(to);
    order.verify(async).failed(any(Exception.class));
  }

  @Test
  public void testTransformLazyResolved() throws Exception {
    doReturn(resolved).when(lazyTransform).get();
    doReturn(failed).when(async).failed(any(Exception.class));

    assertEquals(resolved, base.lazyTransformCancelled(lazyTransform));

    final InOrder order = inOrder(lazyTransform, async);
    order.verify(lazyTransform).get();
    order.verify(async, never()).failed(any(Exception.class));
  }

  @Test
  public void testTransformLazyResolvedThrows() throws Exception {
    doThrow(cause).when(lazyTransform).get();
    doReturn(failed).when(async).failed(any(Exception.class));

    assertEquals(failed, base.lazyTransformCancelled(lazyTransform));

    final InOrder order = inOrder(lazyTransform, async);
    order.verify(lazyTransform).get();
    order.verify(async).failed(any(Exception.class));
  }

  public interface To {
  }
}
