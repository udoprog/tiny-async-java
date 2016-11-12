package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;

public class TinyAsyncRetryUntilResolvedIT {
  private static final long TIMEOUT = 20000;
  private static final Object RESULT = new Object();

  private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

  // setup an direct async framework.
  final FutureFramework async = TinyFuture.builder().threaded(false).scheduler(scheduler).build();

  @Test(timeout = TIMEOUT)
  public void testBasicRetryLogic() throws Exception {
    final AtomicInteger calls = new AtomicInteger();

    final CompletionStage<RetryResult<Object>> f = runRetry(calls, 10000);

    final RetryResult<Object> result = f.join();

    assertEquals(RESULT, result.getResult());
    assertEquals(5, result.getErrors().size());
    assertEquals(ImmutableList.of("doCall 0", "doCall 1", "doCall 2", "doCall 3", "doCall 4"),
        result
            .getErrors()
            .stream()
            .map(Throwable::getCause)
            .map(Throwable::getMessage)
            .collect(Collectors.toList()));
    assertEquals(6, calls.get());
  }

  @Test
  public void testTimeout() throws Exception {
    final AtomicInteger calls = new AtomicInteger();

    final CompletionStage<RetryResult<Object>> f = runRetry(calls, 175);

    try {
      f.join();
    } catch (final Exception e) {
      assertNotNull(e.getCause());

      final Throwable cause = e.getCause();

      assertEquals("doCall 4", cause.getMessage());
      assertEquals(4, cause.getSuppressed().length);
      assertEquals(ImmutableList.of("doCall 0", "doCall 1", "doCall 2", "doCall 3"), Arrays
          .stream(cause.getSuppressed())
          .map(Throwable::getCause)
          .map(Throwable::getMessage)
          .collect(Collectors.toList()));
      return;
    }

    fail("Retry should fail");
  }

  private CompletionStage<RetryResult<Object>> runRetry(
      final AtomicInteger calls, final long timeout
  ) {
    return async.retryUntilCompleted(() -> {
      final int n = calls.getAndIncrement();

      if (n < 5) {
        throw new RuntimeException("doCall " + n);
      }

      return async.completed(RESULT);
    }, RetryPolicy.timed(timeout, TimeUnit.MILLISECONDS,
        RetryPolicy.linear(50, TimeUnit.MILLISECONDS)));
  }
}
