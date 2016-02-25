package eu.toolchain.async;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class TinyAsyncRetryUntilResolved {
    private static final long TIMEOUT = 20000;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    // setup an direct async framework.
    final AsyncFramework async = TinyAsync.builder().threaded(false).scheduler(scheduler).build();

    @Test(timeout = TIMEOUT, expected = ExecutionException.class)
    public void testCallMultipleTimes() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final AsyncFuture<Void> f = async.retryUntilResolved(() -> {
            final int n = calls.getAndIncrement();
            System.out.println("call " + n);
            return async.failed(new RuntimeException("call " + n));
        }, RetryPolicy.timed(1000, RetryPolicy.exponential(100)));

        f.get();
    }
}
