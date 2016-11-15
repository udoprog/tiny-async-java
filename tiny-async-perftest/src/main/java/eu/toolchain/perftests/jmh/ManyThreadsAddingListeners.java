package eu.toolchain.perftests.jmh;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.concurrent.CoreAsync;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2, jvmArgsAppend = "-Djmh.stack.lines=3")
@Warmup(iterations = 2)
@Measurement(iterations = 2)
public class ManyThreadsAddingListeners {
  public static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

  @State(Scope.Benchmark)
  public static class ThreadPool {
    final ExecutorService executor = Executors.newWorkStealingPool(THREAD_COUNT);
    final Async async = CoreAsync.builder().executor(executor).build();
    final ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(executor);
  }

  @Param({"1", "5", "10"})
  public int size;

  @Param({"1", "10", "100", "1000", "10000"})
  public int callbackCount;

  @Benchmark
  public int tiny(final ThreadPool pool) throws Exception {
    final int expectedSum = ((size * (size - 1)) / 2) * callbackCount * THREAD_COUNT;

    final AtomicInteger sum = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch tasks = new CountDownLatch(callbackCount * size * THREAD_COUNT);

    final Consumer<Integer> callback = result -> {
      sum.addAndGet(result);
      tasks.countDown();
    };

    for (int i = 0; i < size; i++) {
      final int current = i;

      final Stage<Integer> future = pool.async.call(() -> {
        latch.await();
        return current;
      });

      for (int t = 0; t < THREAD_COUNT; t++) {
        pool.executor.execute(() -> {
          for (int c = 0; c < callbackCount; c++) {
            future.whenComplete(callback);
          }
        });
      }
    }

    latch.countDown();
    tasks.await();

    if (sum.get() != expectedSum) {
      throw new IllegalStateException(
          String.format("did not properly collect all values: expected %d, but was %d",
              expectedSum, sum.get()));
    }

    return expectedSum;
  }

  @Benchmark
  public int guava(final ThreadPool pool) throws Exception {
    final int expectedSum = ((size * (size - 1)) / 2) * callbackCount * THREAD_COUNT;

    final AtomicInteger sum = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch tasks = new CountDownLatch(callbackCount * size * THREAD_COUNT);

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

    for (int i = 0; i < size; i++) {
      final int current = i;

      final ListenableFuture<Integer> future = pool.listeningExecutor.submit(() -> {
        latch.await();
        return current;
      });

      for (int t = 0; t < THREAD_COUNT; t++) {
        pool.executor.execute(() -> {
          for (int c = 0; c < callbackCount; c++) {
            com.google.common.util.concurrent.Futures.addCallback(future, callback);
          }
        });
      }
    }

    latch.countDown();
    tasks.await();

    if (sum.get() != expectedSum) {
      throw new IllegalStateException(
          String.format("did not properly collect all values: expected %d, but was %d",
              expectedSum, sum.get()));
    }

    return expectedSum;
  }
}
