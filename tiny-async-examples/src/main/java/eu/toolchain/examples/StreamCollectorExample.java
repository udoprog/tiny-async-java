package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing using a {@code StreamCollector}.
 */
public class StreamCollectorExample {
  public static void main(String[] argv) throws InterruptedException, ExecutionException {
    final Async async = Helpers.setup();

    final List<Stage<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      final int value = i;

      futures.add(async.call(() -> value));
    }

    final Stage<Integer> sum =
        async.endCollect(futures, (completed, failed, cancelled) -> completed);

    System.out.println("result: " + sum.join());
  }
}
