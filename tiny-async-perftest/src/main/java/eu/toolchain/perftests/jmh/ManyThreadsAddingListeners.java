package eu.toolchain.perftests.jmh;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.CoreAsync;
import eu.toolchain.concurrent.Async;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.openjdk.jmh.annotations.Benchmark;

public class ManyThreadsAddingListeners {
  private static final int SIZE = 1;
  private static final int CALLBACK_COUNT = 10000;
  private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
  private static final int EXPECTED_SUM = ((SIZE * (SIZE - 1)) / 2) * CALLBACK_COUNT * THREAD_COUNT;

  @Benchmark
  public void tiny() throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    final Async async = CoreAsync.builder().executor(executor).build();

    final AtomicInteger sum = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch tasks = new CountDownLatch(CALLBACK_COUNT * THREAD_COUNT * SIZE);

    final Consumer<Integer> callback = result -> {
      sum.addAndGet(result);
      tasks.countDown();
    };

    for (int i = 0; i < SIZE; i++) {
      final int current = i;

      final CompletionStage<Integer> future = async.call(() -> {
        latch.await();
        return current;
      });

      for (int t = 0; t < THREAD_COUNT; t++) {
        executor.execute(() -> {
          for (int c = 0; c < CALLBACK_COUNT; c++) {
            future.whenCompleted(callback);
          }
        });
      }
    }

    latch.countDown();
    tasks.await();

    if (sum.get() != EXPECTED_SUM) {
      throw new IllegalStateException(
          String.format("did not properly collect all values: expected %d, but was %d",
              EXPECTED_SUM, sum.get()));
    }

    executor.shutdown();
  }

  @Benchmark
  public void guava() throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    final ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(executor);

    final AtomicInteger sum = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch tasks = new CountDownLatch(CALLBACK_COUNT * THREAD_COUNT * SIZE);

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

      final ListenableFuture<Integer> future = listeningExecutor.submit(() -> {
        latch.await();
        return current;
      });

      for (int t = 0; t < THREAD_COUNT; t++) {
        executor.execute(() -> {
          for (int c = 0; c < CALLBACK_COUNT; c++) {
            com.google.common.util.concurrent.Futures.addCallback(future, callback);
          }
        });
      }
    }

    latch.countDown();
    tasks.await();

    if (sum.get() != EXPECTED_SUM) {
      throw new IllegalStateException(
          String.format("did not properly collect all values: expected %d, but was %d",
              EXPECTED_SUM, sum.get()));
    }

    listeningExecutor.shutdown();
  }
}
