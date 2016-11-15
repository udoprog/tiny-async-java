package eu.toolchain.perftests;

import com.google.common.base.Stopwatch;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.CoreAsync;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FutureVsSync {
  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

  private static final ExecutorService asyncThreads =
      Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);
  private static final ExecutorService syncThreads =
      Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);

  private static final Async async = CoreAsync.builder().executor(asyncThreads).build();

  public static void main(String argv[]) throws Exception {
    final long syncTime;
    final double sync;
    final long asyncTime;
    final double async;

    {
      Stopwatch sw = Stopwatch.createStarted();
      sync = sync();
      syncTime = sw.elapsed(TimeUnit.NANOSECONDS);
    }

    {
      Stopwatch sw = Stopwatch.createStarted();
      async = async();
      asyncTime = sw.elapsed(TimeUnit.NANOSECONDS);
    }

    System.out.println(String.format("sync: %f (%d ns)", sync, syncTime));
    System.out.println(String.format("async: %f (%d ns)", async, asyncTime));
    System.exit(0);
  }

  private static Function<Collection<Double>, Double> summer = results -> {
    double sum = 0.0d;

    for (final Double r : results) {
      sum += r;
    }

    return sum;
  };

  private static double async() throws Exception {
    final List<Stage<Double>> outer = new ArrayList<>();

    for (int i = 0; i < AVAILABLE_PROCESSORS - 1; i++) {
      outer.add(someAsyncCall());
    }

    return async.collect(outer, summer).join();
  }

  private static Stage<Double> someAsyncCall() {
    final List<Stage<Double>> inner = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      inner.add(async.call(FutureVsSync::doSomeWork));
    }

    return async.collect(inner, summer);
  }

  private static double sync() throws Exception {
    final List<Future<Double>> outer = new ArrayList<>();

    for (int i = 0; i < AVAILABLE_PROCESSORS - 1; i++) {
      outer.add(someSyncCall());
    }

    double sum = 0.0d;

    for (final Future<Double> f : outer) {
      sum += f.get();
    }

    return sum;
  }

  private static Future<Double> someSyncCall() {
    return syncThreads.submit(() -> {
      final List<Future<Double>> inner = new ArrayList<>();

      for (int i = 0; i < 100; i++) {
        inner.add(syncThreads.submit(new Callable<Double>() {
          @Override
          public Double call() throws Exception {
            return doSomeWork();
          }
        }));
      }

      double sum = 0.0d;

      for (final Future<Double> f : inner) {
        sum += f.get();
      }

      return sum;
    });
  }

  public static double doSomeWork() {
    double sum = 0.0d;

    for (int i = 0; i < 1000000; i++) {
      sum += Math.sqrt(Math.pow(i, 2));
    }

    return sum;
  }
}
