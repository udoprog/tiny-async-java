package eu.toolchain.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestEventuallyCollect {
    private ExecutorService executor;
    private ExecutorService otherExecutor;
    private AsyncFramework async;

    @Before
    public void setup() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

    @Test
    public void testBasic() throws Exception {
        final List<Callable<AsyncFuture<Integer>>> callables = new ArrayList<>();
        final AtomicInteger pending = new AtomicInteger();
        final AtomicInteger called = new AtomicInteger();

        for (int i = 0; i < 10000; i++) {
            final int n = i;

            callables.add(new Callable<AsyncFuture<Integer>>() {
                @Override
                public AsyncFuture<Integer> call() throws Exception {
                    pending.incrementAndGet();

                    return async.call(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            if (pending.decrementAndGet() >= 10)
                                throw new IllegalStateException("bad stuff");

                            called.incrementAndGet();
                            return n;
                        }
                    }, otherExecutor);
                }
            });
        }

        final AsyncFuture<Integer> res = async.eventuallyCollect(callables, new StreamCollector<Integer, Integer>() {
            final AtomicInteger sum = new AtomicInteger();

            @Override
            public void resolved(Integer result) throws Exception {
                sum.addAndGet(result);
            }

            @Override
            public void failed(Throwable cause) throws Exception {
            }

            @Override
            public void cancelled() throws Exception {
            }

            @Override
            public Integer end(int resolved, int failed, int cancelled) throws Exception {
                return sum.get();
            }
        }, 14);

        System.out.println(res.get());
        System.out.println(called.get());
    }
}