package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class CollectAndDiscardHelperTest {
  private static final int size = 2;

  private static final Object result = new Object();
  private static final Throwable cause = new Exception();

  private CompletableFuture<Void> target;

  private CollectAndDiscardHelper helper;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    target = mock(CompletableFuture.class);
    helper = new CollectAndDiscardHelper(size, target);
  }

  private void verifyTarget(int resolved, int failed, int cancelled) {
    verify(target, times(resolved)).complete(null);
    verify(target, times(failed)).fail(any(Exception.class));
    verify(target, times(cancelled)).cancel();
  }

  @Test
  public void testResolved() throws Exception {
    helper.completed(result);
    helper.completed(result);

    verifyTarget(1, 0, 0);
  }

  @Test
  public void testFailed() throws Exception {
    helper.failed(cause);
    helper.completed(result);

    verifyTarget(0, 1, 0);
  }

  @Test
  public void testCancelled() throws Exception {
    helper.completed(result);
    helper.cancelled();

    verifyTarget(0, 0, 1);
  }
}
