package eu.toolchain.async;

import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.MoreExecutors;
import eu.toolchain.async.RecursionSafeAsyncCaller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.lang.ThreadLocal;
import java.lang.Thread;

import com.google.common.util.concurrent.AtomicLongMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RecursionSafeAsyncCallerIntegrationTest {

    private AtomicLongMap<Long> recursionDepthPerThread;
    private AtomicLongMap<Long> maxRecursionDepthPerThread;
    AtomicLong totIterations;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        recursionDepthPerThread = AtomicLongMap.create();
        maxRecursionDepthPerThread = AtomicLongMap.create();
        totIterations = new AtomicLong(0);
    }


    public void testBasicRecursionMethod(RecursionSafeAsyncCaller caller, ConcurrentLinkedQueue<Integer> testData) {

        class RecursionRunnable implements Runnable {
            RecursionSafeAsyncCaller caller;
            ConcurrentLinkedQueue<Integer> testData;

            RecursionRunnable(RecursionSafeAsyncCaller caller, ConcurrentLinkedQueue<Integer> testData) {
                this.caller = caller;
                this.testData = testData;
            }

            @Override
            public void run() {
                Long threadId = Thread.currentThread().getId();
                Long currDepth = recursionDepthPerThread.addAndGet(threadId, 1L);
                Long currMax = maxRecursionDepthPerThread.get(threadId);
                if (currDepth > currMax) {
                    maxRecursionDepthPerThread.put(threadId, currDepth);
                }

                if (testData.size() == 0)
                    return;
                testData.poll();
                totIterations.incrementAndGet();

                // Recursive call, via caller
                testBasicRecursionMethod(caller, testData);

                recursionDepthPerThread.addAndGet(threadId, -1L);
            }
        };

        RecursionRunnable runnable = new RecursionRunnable(caller, testData);
        caller.execute(runnable);
    }

    @Test
    public void testBasic() throws Exception {
        final long MAX_RECURSION_DEPTH = 2;
        ExecutorService executorServiceReal = Executors.newFixedThreadPool(10);
        AsyncCaller caller2 = mock(AsyncCaller.class);
        RecursionSafeAsyncCaller recursionCaller =
                new RecursionSafeAsyncCaller(executorServiceReal, caller2, MAX_RECURSION_DEPTH);
        ConcurrentLinkedQueue<Integer>
            testData = new ConcurrentLinkedQueue<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        testBasicRecursionMethod(recursionCaller, testData);

        // Wait for recursive calls in thread pool to create new work until done, or timeout.
        // The executorServiceReal.shutdown() below is not enough since potentially some new work
        // is still on the way in, due to recursive nature of the test.
        long startTime = System.currentTimeMillis();
        long maxTime = 1000;
        while (testData.size() > 0) {
            if (System.currentTimeMillis() - startTime > maxTime) {
                // Timeout, test will fail in the assert below
                break;
            }
            Thread.sleep(10);
        }

        executorServiceReal.shutdown();
        executorServiceReal.awaitTermination(1000, TimeUnit.MILLISECONDS);

        assert(testData.size() == 0);
        assert(totIterations.get() == 10);

        Long maxStackDepth=-1L;
        Map<Long, Long> readOnlyMap = maxRecursionDepthPerThread.asMap();
        for (Long key : readOnlyMap.keySet()) {
            Long val = readOnlyMap.get(key);
            if (val > maxStackDepth)
                maxStackDepth = val;
        }
        assert(maxStackDepth != -1L);

        // Checking with +1 since our initial call to testBasicRecursionMethod() above adds 1
        assert(maxStackDepth <= MAX_RECURSION_DEPTH+1);
    }

    @Test(expected = StackOverflowError.class)
    public void testRecursionsFailure() throws Exception {
        doRecursions(false);
    }

    @Test
    public void testRecursionsSuccess() throws Exception {
        doRecursions(true);
    }

    private void doRecursions(final boolean recursionSafeAsyncCaller)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        final AsyncFramework async = TinyAsync
                .builder()
                .callerExecutor(Executors.newSingleThreadExecutor())
                .recursionSafeAsyncCaller(recursionSafeAsyncCaller)
                .build();

        final ResolvableFuture<Integer> source = async.future();

        AsyncFuture<Integer> tail = source;

        // 100k should blow up the stack without the recursionSafeAsyncCaller
        for (int i = 0; i < 100000; i++) {
            tail = tail.lazyTransform(value -> {
                // immediate future
                return async.resolved(value + 1);
            });
        }

        source.resolve(0);
        assertEquals(100000, (int) tail.get());
    }
}
