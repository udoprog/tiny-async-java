package eu.toolchain.examples;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Start of an example to attempt to test how number of threads scale.
 * <p>
 * Note: I've yet to be successful in a representative example of context switching.
 */
public class ThreadScaling {
  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

  private final static HowManyThreads howManyThreads = new HowManyThreads();

  public static void main(String argv[]) throws Exception {
    if (AVAILABLE_PROCESSORS == 1) {
      System.out.println("Your computer only has one physical processor");
      return;
    }

    howManyThreads.run();
  }

  public static double someWork(int iterations) {
    double sum = 0.0d;

    for (int i = 0; i < iterations; i++) {
      sum += Math.sqrt(Math.pow(i, 2));
    }

    return sum;
  }

  private static class HowManyThreads {
    private static final int WORK_ITERATIONS = 10;
    private static final int START_THREADS = 0;
    private static final int MAX_THREADS = 20000;
    private static final int STEP = 2000;
    private static final long WORK_PER_THREAD = 10000;

    public void run() throws Exception {
      System.out.println("How does number of threads correspond to speed of computation?");

      final XYSeriesCollection dataset1 = new XYSeriesCollection();
      final XYSeriesCollection dataset2 = new XYSeriesCollection();

      final int sleep = 50;

      {
        System.out.println(String.format("Calculating w/ %d%% sleepy threads", sleep));
        final List<Timing> times = runTest(sleep);

        dataset1.addSeries(timingsToSeries(String.format("%d%%", sleep), times));
        dataset2.addSeries(switchesToSeries(String.format("switches %d%%", sleep), times));
      }

      ChartUtils.showChart("How many threads", "# threads", "milliseconds", dataset1, "switches",
          dataset2);
    }

    private XYSeries timingsToSeries(String title, List<Timing> times) {
      XYSeries series = new XYSeries(title);

      for (final Timing t : times) {
        series.add(t.getThreads(),
            TimeUnit.MILLISECONDS.convert(t.getElapsedNanos(), TimeUnit.NANOSECONDS));
      }

      return series;
    }

    private XYSeries switchesToSeries(String title, List<Timing> times) {
      XYSeries series = new XYSeries(title);

      for (final Timing t : times) {
        series.add(t.getThreads(), t.getSwitches());
      }

      return series;
    }

    private List<Timing> runTest(final int sleep) throws InterruptedException, ExecutionException {
      final List<Timing> times = new ArrayList<>();

      final long sleepProportion;

      if (sleep == 0) {
        sleepProportion = 0;
      } else {
        final double s = sleep / 100.0;
        sleepProportion = (long) (TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS) / s) -
            TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS);
      }

      int known = -1;

      final int end = MAX_THREADS + AVAILABLE_PROCESSORS;
      final long howMuchWork = end * WORK_PER_THREAD;

      for (
          int threads = START_THREADS + AVAILABLE_PROCESSORS; threads <= end; threads += STEP
          ) {
        int progress = (int) (100 * threads / (double) MAX_THREADS);

        if (known != progress) {
          System.out.print(String.format("%d%% ", progress));
          System.out.flush();
          known = progress;
        }

        final ExecutorService service = Executors.newFixedThreadPool(threads);

        final AtomicLong workCounter = new AtomicLong();
        final List<Future<?>> jobs = new ArrayList<>();

        final CountDownLatch checkIn = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);

        final AtomicInteger switches = new AtomicInteger();

        final int counts[] = new int[threads];

        for (int i = 0; i < threads; i++) {
          final int id = i;

          jobs.add(service.submit(() -> {
            double sum = 0.0d;

            long total = 0;

            while (true) {
              checkIn.countDown();
              start.await();

              if (workCounter.getAndIncrement() >= howMuchWork) {
                break;
              }

              counts[id]++;

              final long start1 = System.nanoTime();
              sum += someWork(WORK_ITERATIONS);
              total += System.nanoTime() - start1;

              if (sleepProportion == 0) {
                continue;
              }

              while (total >= sleepProportion) {
                // this thread will yield time for other threads.
                switches.getAndIncrement();
                total -= sleepProportion;
                Thread.sleep(1);
              }
            }

            return sum;
          }));
        }

        // ask the vm nicely to _try_ to perform gc's before each run.
        Runtime.getRuntime().gc();

        final Stopwatch sw = Stopwatch.createStarted();

        checkIn.await();
        start.countDown();

        for (final Future<?> f : jobs) {
          f.get();
        }

        times.add(new Timing(threads, sw.elapsed(TimeUnit.NANOSECONDS), switches.get()));

        for (int i = 0; i < threads; i++) {
          System.out.print(counts[i] + " ");

          if (i % 100 == 0) {
            System.out.println();
          }
        }

        System.out.println();

        // ask the vm nicely to _try_ to perform gc's after each run.
        Runtime.getRuntime().gc();

        service.shutdown();
      }

      Collections.sort(times, (a, b) -> Long.compare(a.elapsedNanos, b.elapsedNanos));

      System.out.println();
      return times;
    }

    @Data
    private static final class Timing {
      private final int threads;
      private final long elapsedNanos;
      private final long switches;
    }
  }
}
