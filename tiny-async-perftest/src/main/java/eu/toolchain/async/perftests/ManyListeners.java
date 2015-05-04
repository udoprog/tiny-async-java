package eu.toolchain.async.perftests;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureResolved;
import eu.toolchain.async.TestCase;
import eu.toolchain.async.TinyAsync;

public class ManyListeners {
    private static final int SIZE = 10;
    private static final int CALLBACK_COUNT = 1000;
    private static final int EXPECTED_SUM = ((SIZE * (SIZE - 1)) / 2) * CALLBACK_COUNT;

    private static int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    public static class Tiny implements TestCase {
        @Override
        public void test() throws Exception {
            ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors
                    .newFixedThreadPool(THREAD_COUNT));

            final AsyncFramework async = TinyAsync.builder().executor(service).build();

            final AtomicInteger sum = new AtomicInteger();

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch tasks = new CountDownLatch(CALLBACK_COUNT * SIZE);

            final FutureResolved<Integer> callback = new FutureResolved<Integer>() {
                @Override
                public void resolved(Integer result) {
                    sum.addAndGet(result);
                    tasks.countDown();
                }
            };

            for (int i = 0; i < SIZE; i++) {
                final int current = i;

                final AsyncFuture<Integer> future = async.call(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        latch.await();
                        return current;
                    }
                });

                for (int c = 0; c < CALLBACK_COUNT; c++)
                    future.on(callback);
            }

            latch.countDown();
            tasks.await(10, TimeUnit.SECONDS);

            if (sum.get() != EXPECTED_SUM)
                throw new IllegalStateException(String.format(
                        "did not properly collect all values: expected %d, but was %d", EXPECTED_SUM, sum.get()));

            service.shutdown();
        }
    }

    public static class Guava implements TestCase {
        @Override
        public void test() throws Exception {
            ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors
                    .newFixedThreadPool(THREAD_COUNT));

            final AtomicInteger sum = new AtomicInteger();

            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch tasks = new CountDownLatch(CALLBACK_COUNT * SIZE);

            final FutureCallback<Integer> callback = new FutureCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    sum.addAndGet(result);
                    tasks.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                }
            };

            for (int i = 0; i < SIZE; i++) {
                final int current = i;

                final ListenableFuture<Integer> future = service.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        latch.await();
                        return current;
                    }
                });

                for (int c = 0; c < CALLBACK_COUNT; c++) {
                    Futures.addCallback(future, callback);
                }
            }

            latch.countDown();
            tasks.await(10, TimeUnit.SECONDS);

            if (sum.get() != EXPECTED_SUM)
                throw new IllegalStateException(String.format(
                        "did not properly collect all values: expected %d, but was %d", EXPECTED_SUM, sum.get()));

            service.shutdown();
        }
    }
}