package eu.toolchain.async;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TinyAsyncRetryUntilResolvedIT {
    private static final long TIMEOUT = 20000;
    private static final Object RESULT = new Object();

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    // setup an direct async framework.
    final AsyncFramework async = TinyAsync.builder().threaded(false).scheduler(scheduler).build();

    @Test(timeout = TIMEOUT)
    public void testBasicRetryLogic() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final AsyncFuture<RetryResult<Object>> f = runRetry(calls, 10000);

        final RetryResult<Object> result = f.get();

        assertEquals(RESULT, result.getResult());
        assertEquals(5, result.getErrors().size());
        assertEquals(ImmutableList.of("call 0", "call 1", "call 2", "call 3", "call 4"), result
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

        final AsyncFuture<RetryResult<Object>> f = runRetry(calls, 175);

        try {
            f.get();
        } catch (final Exception e) {
            assertNotNull(e.getCause());

            final Throwable cause = e.getCause();

            assertEquals("call 4", cause.getMessage());
            assertEquals(4, cause.getSuppressed().length);
            assertEquals(ImmutableList.of("call 0", "call 1", "call 2", "call 3"), Arrays
                .stream(cause.getSuppressed())
                .map(Throwable::getCause)
                .map(Throwable::getMessage)
                .collect(Collectors.toList()));
            return;
        }

        fail("Retry should fail");
    }

    private AsyncFuture<RetryResult<Object>> runRetry(
        final AtomicInteger calls, final long timeout
    ) {
        return async.retryUntilResolved(() -> {
            final int n = calls.getAndIncrement();

            if (n < 5) {
                throw new RuntimeException("call " + n);
            }

            return async.resolved(RESULT);
        }, RetryPolicy.timed(timeout, RetryPolicy.linear(50)));
    }
}
