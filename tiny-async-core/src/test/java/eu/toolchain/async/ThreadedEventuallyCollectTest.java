package eu.toolchain.async;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A high-level integration test for {@code TinyAsync#eventuallyCollect(java.util.Collection, StreamCollector, int)}.
 */
public class ThreadedEventuallyCollectTest {
    private ExecutorService executor;
    private ExecutorService otherExecutor;
    private AsyncFramework async;

    private static final long COUNT = 1000;
    private static final long EXPECTED_SUM = COUNT;
    private static final int PARALLELISM = 4;

    @Before
    public void setup() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
        otherExecutor = Executors.newFixedThreadPool(10);

        async = TinyAsync.builder().executor(executor).build();
    }

    @After
    public void teardown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(100, TimeUnit.MILLISECONDS);

        otherExecutor.shutdown();
        otherExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 500)
    public void testBasic() throws Exception {
        int attempt = 0;

        while (attempt++ < 10) {
            final List<Callable<AsyncFuture<Long>>> callables = new ArrayList<>();
            final AtomicInteger pending = new AtomicInteger();
            final AtomicInteger called = new AtomicInteger();

            for (long i = 0; i < COUNT; i++) {
                callables.add(new Callable<AsyncFuture<Long>>() {
                    @Override
                    public AsyncFuture<Long> call() throws Exception {
                        pending.incrementAndGet();

                        return async.call(new Callable<Long>() {
                            @Override
                            public Long call() throws Exception {
                                if (pending.decrementAndGet() >= PARALLELISM)
                                    throw new IllegalStateException("bad stuff");

                                called.incrementAndGet();
                                return 1l;
                            }
                        }, otherExecutor);
                    }
                });
            }

            final AsyncFuture<Long> res = async.eventuallyCollect(callables, new StreamCollector<Long, Long>() {
                final AtomicLong sum = new AtomicLong();

                @Override
                public void resolved(Long result) throws Exception {
                    sum.addAndGet(result);
                }

                @Override
                public void failed(Throwable cause) throws Exception {
                }

                @Override
                public void cancelled() throws Exception {
                }

                @Override
                public Long end(int resolved, int failed, int cancelled) throws Exception {
                    return sum.get();
                }
            }, PARALLELISM);

            assertEquals(EXPECTED_SUM, (long) res.get());
            assertEquals(COUNT, called.get());
        }
    }
}