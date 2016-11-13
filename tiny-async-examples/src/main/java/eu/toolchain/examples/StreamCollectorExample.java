package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.StreamCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example application showcasing using a {@code StreamCollector}.
 */
public class StreamCollectorExample {
  public static void main(String[] argv) throws InterruptedException, ExecutionException {
    final Async async = Helpers.setup();

    final List<CompletionStage<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      final int value = i;

      futures.add(async.call(() -> value));
    }

    final CompletionStage<Integer> sum =
        async.collect(futures, new StreamCollector<Integer, Integer>() {
          private final AtomicInteger result = new AtomicInteger();

          @Override
          public void completed(Integer result) {
            this.result.addAndGet(result);
          }

          @Override
          public void failed(Throwable cause) {
          }

          @Override
          public void cancelled() {
          }

          @Override
          public Integer end(int resolved, int failed, int cancelled) {
            return this.result.get();
          }
        });

    System.out.println("result: " + sum.join());
  }
}
