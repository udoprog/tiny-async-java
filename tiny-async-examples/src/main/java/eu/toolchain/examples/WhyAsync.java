package eu.toolchain.examples;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Collector;
import eu.toolchain.async.TinyAsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Why async should be considered over sync.
 * <p>
 * Important Note: remember to read the notes about each example, especially the 'Important Note'
 * section.
 *
 * @author udoprog
 */
public class WhyAsync {
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private final static TrickyThreadScheduling trickyThreadScheduling =
        new TrickyThreadScheduling();

    public static void main(String argv[]) throws Exception {
        if (AVAILABLE_PROCESSORS == 1) {
            System.out.println("Your computer only has one physical processor");
            return;
        }

        trickyThreadScheduling.run();
    }

    public static double someWork(int iterations) {
        double sum = 0.0d;

        for (int i = 0; i < iterations; i++) {
            sum += Math.sqrt(Math.pow(i, 2));
        }

        return sum;
    }

    /**
     * The thread pool that you use in your application has the same number of threads as the number
     * of cores on your system. The async approach will make better use of them by the virtue of
     * only waking up threads when necessary (i.e. an interesting event happens).
     * <p>
     * As a bonus, the synchronous approach will (most likely) deadlock if we change
     * NUMBER_OF_REQUESTS above, to the same as AVAILABLE_PROCESSORS, because the loop that waits
     * for the work to finish will occupy all threads. The async approach will happily work on a
     * single thread if necessary.
     * <p>
     * So how many threads would the synchronous count require? At least AVAILABLE_PROCESSORS +
     * NUMBER_OF_REQUESTS, since currently NUMBER_OF_REQUESTS threads will be occupied waiting for
     * results.
     * <p>
     * Another solution could be to use multiple, distinct thread pools for each logical component,
     * that will increase the number of threads, pools, and contention.
     * <p>
     * Important Note: summing below is a contrived example, a better approach would arguably be to
     * pass around an AtomicDouble. Consider these computations that uses more complex types of
     * objects instead (like rows from a database), where there are no atomic numbers available.
     */
    private static class TrickyThreadScheduling {
        private static final int WORK_ITERATIONS = 100000;
        private static final int NUMBER_OF_REQUESTS = AVAILABLE_PROCESSORS - 1;
        private static final int NUMBER_OF_ASYNC_THREADS = AVAILABLE_PROCESSORS;
        private static final int NUMBER_OF_SYNC_THREADS = AVAILABLE_PROCESSORS;
        private static final int INNER_REQUEST_COUNT = 100;

        private static final ExecutorService asyncThreads =
            Executors.newFixedThreadPool(NUMBER_OF_ASYNC_THREADS,
                new ThreadFactoryBuilder().setNameFormat("async-%d").build());
        private static final ExecutorService syncThreads =
            Executors.newFixedThreadPool(NUMBER_OF_SYNC_THREADS,
                new ThreadFactoryBuilder().setNameFormat("sync-%d").build());

        private static final AsyncFramework async =
            TinyAsync.builder().executor(asyncThreads).build();

        private void run() throws Exception {
            System.out.println("Async making better use of the given threads.");

            final long syncTime;
            final double sync;
            final long asyncTime;
            final double async;

            {
                Stopwatch sw = Stopwatch.createStarted();
                async = async();
                asyncTime = sw.elapsed(TimeUnit.NANOSECONDS);

                System.out.println(String.format("async: %f (%d ns)", async, asyncTime));
            }

            {
                Stopwatch sw = Stopwatch.createStarted();

                if (NUMBER_OF_REQUESTS >= NUMBER_OF_SYNC_THREADS) {
                    System.out.println("WARNING: DEADLOCK IMMINENT");
                }

                sync = sync();
                syncTime = sw.elapsed(TimeUnit.NANOSECONDS);

                System.out.println(String.format("sync : %f (%d ns)", sync, syncTime));
            }
        }

        private Collector<Double, Double> summer = new Collector<Double, Double>() {
            @Override
            public Double collect(Collection<Double> results) throws Exception {
                double sum = 0.0d;

                for (final Double r : results) {
                    sum += r;
                }

                return sum;
            }
        };

        private double async() throws Exception {
            final List<AsyncFuture<Double>> outer = new ArrayList<>();

            for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {
                outer.add(someAsyncCall());
            }

            return async.collect(outer, summer).get();
        }

        private AsyncFuture<Double> someAsyncCall() {
            return async.lazyCall(new Callable<AsyncFuture<Double>>() {
                @Override
                public AsyncFuture<Double> call() throws Exception {
                    final List<AsyncFuture<Double>> inner = new ArrayList<>();

                    for (int i = 0; i < INNER_REQUEST_COUNT; i++) {
                        inner.add(async.call(new Callable<Double>() {
                            @Override
                            public Double call() throws Exception {
                                return someWork(WORK_ITERATIONS);
                            }
                        }));
                    }

                    return async.collect(inner, summer);
                }
            });
        }

        private double sync() throws Exception {
            final List<Future<Double>> outer = new ArrayList<>();

            for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {
                outer.add(someSyncCall());
            }

            double sum = 0.0d;

            for (final Future<Double> f : outer) {
                sum += f.get();
            }

            return sum;
        }

        private Future<Double> someSyncCall() {
            return syncThreads.submit(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    final List<Future<Double>> inner = new ArrayList<>();

                    for (int i = 0; i < INNER_REQUEST_COUNT; i++) {
                        inner.add(syncThreads.submit(new Callable<Double>() {
                            @Override
                            public Double call() throws Exception {
                                return someWork(WORK_ITERATIONS);
                            }
                        }));
                    }

                    double sum = 0.0d;

                    // this is equivalent to all threads blocking for the above result to finish
                    // for some reason.
                    for (final Future<Double> f : inner) {
                        sum += f.get();
                    }

                    return sum;
                }
            });
        }
    }
}
