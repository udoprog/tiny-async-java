package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.examples.helpers.Helpers;
import java.util.concurrent.ExecutionException;

/**
 * An example application that showcases subscription of events on an {@code AsyncFuture}.
 */
public class Blocking {
  public static void main(String argv[]) throws InterruptedException, ExecutionException {
    final Async async = Helpers.setup();

    final Stage<Integer> f = async.call(() -> {
      Thread.sleep(1000);
      return 10;
    });

    System.out.println("result: " + f.join());
  }
}
