package eu.toolchain.async;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TinyAsyncCollectIntegrationTest {
    private static final Object REF = new Object();
    private static final Exception A = new Exception("A");
    private static final Exception B = new Exception("B");

    private static ExecutorService executor;
    private static TinyAsync async;

    private static final long TIMEOUT = 5000;

    private static final int COUNT = 10;
    private static final int BATCH_SIZE = 100;

    @BeforeClass
    public static void beforeClass() {
        executor = Executors.newFixedThreadPool(10);
        async = TinyAsync.builder().executor(executor).build();
    }

    @AfterClass
    public static void afterClass() {
        executor.shutdown();
    }

    // TODO: move out from unit tests.
    @Test(timeout = TIMEOUT)
    public void testEmpty() throws InterruptedException, ExecutionException {
        final List<AsyncFuture<Exception>> futures = new ArrayList<>();

        final AsyncFuture<Object> result =
            async.collect(futures, new Collector<Exception, Object>() {
                @Override
                public Object collect(Collection<Exception> results) throws Exception {
                    Assert.assertEquals(0, results.size());
                    return REF;
                }
            });

        Assert.assertEquals(REF, result.get());
    }

    // TODO: move out from unit tests.
    @Test(timeout = TIMEOUT)
    public void testResolved() throws InterruptedException, ExecutionException {
        final List<AsyncFuture<Exception>> futures = new ArrayList<>();
        futures.add(async.resolved(A));
        futures.add(async.resolved(B));

        final AsyncFuture<Object> result =
            async.collect(futures, new Collector<Exception, Object>() {
                @Override
                public Object collect(Collection<Exception> results) throws Exception {
                    final List<Exception> r = new ArrayList<>(results);
                    Assert.assertEquals(2, r.size());
                    Assert.assertTrue(r.get(0) != r.get(1));
                    Assert.assertTrue(r.get(0) == A || r.get(0) == B);
                    Assert.assertTrue(r.get(1) == A || r.get(1) == B);
                    return REF;
                }
            });

        Assert.assertEquals(REF, result.get());
    }

    // TODO: move out from unit tests.
    @Test(timeout = TIMEOUT)
    public void testErrors() throws InterruptedException, ExecutionException {
        final List<AsyncFuture<Object>> futures = new ArrayList<>();
        futures.add(async.failed(A));
        futures.add(async.failed(B));

        final AsyncFuture<Object> result = async.collect(futures, new Collector<Object, Object>() {
            @Override
            public Object collect(Collection<Object> results) throws Exception {
                return REF;
            }
        });

        try {
            result.get();
        } catch (ExecutionException e) {
            Assert.assertNotNull(e.getCause());
            final Throwable[] s = e.getCause().getSuppressed();
            Assert.assertEquals(2, s.length);
            Assert.assertTrue(s[0] != s[1]);
            Assert.assertTrue("A should be in array of supressed", A == s[0] || A == s[1]);
            Assert.assertTrue("B should be in array of supressed", B == s[0] || B == s[1]);
            return;
        }

        Assert.fail("ExecutionException was not thrown");
    }

    // TODO: move out from unit tests.
    @Test(timeout = TIMEOUT)
    public void testThreadedReduce() throws InterruptedException, ExecutionException {
        for (int i = 0; i < COUNT; i++) {
            final CountDownLatch latch = new CountDownLatch(1);

            final List<AsyncFuture<Integer>> futures = new ArrayList<>();

            for (int j = 0; j < BATCH_SIZE; j++) {
                final int p = i * COUNT + j;

                futures.add(async.call(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        latch.await();
                        return p;
                    }
                }));
            }

            final AsyncFuture<List<Integer>> future =
                async.collect(futures, new Collector<Integer, List<Integer>>() {
                    @Override
                    public List<Integer> collect(Collection<Integer> results) throws Exception {
                        return new ArrayList<>(results);
                    }
                });

            // for the horde!
            latch.countDown();

            final List<Integer> results = future.get();
            final List<Integer> sorted = new ArrayList<>(results);

            Collections.sort(sorted);
            final Iterator<Integer> iter = sorted.iterator();

            for (int j = 0; j < BATCH_SIZE; j++) {
                Assert.assertEquals((Integer) (i * COUNT + j), iter.next());
            }
        }
    }

    @Test
    public void testSomething() {
        final ListeningExecutorService executor =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        final CountDownLatch latch = new CountDownLatch(1);

        final ListenableFuture<Object> future = executor.submit(() -> {
            latch.await();
            System.out.println("ok, go");
            return new Object();
        });

        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                System.out.println("success");
            }

            @Override
            public void onFailure(final Throwable t) {
                System.out.println("failure");
                t.printStackTrace(System.out);
            }
        });

        future.cancel(false);
        latch.countDown();
    }
}
