package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.examples.helpers.Helpers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example application showcasing using a {@code StreamCollector}.
 */
public class StreamCollect {
  public static void main(String[] argv) throws InterruptedException, ExecutionException {
    final Async async = Helpers.setup();

    final List<Stage<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      final int value = i;

      futures.add(async.call(() -> value));
    }

    final AtomicInteger integer = new AtomicInteger();

    final Stage<Integer> sum =
        async.streamCollect(futures, integer::addAndGet, integer::get);

    System.out.println("result: " + sum.join());
  }
}
