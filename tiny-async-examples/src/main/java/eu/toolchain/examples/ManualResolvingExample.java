package eu.toolchain.examples;

import eu.toolchain.concurrent.Completable;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.concurrent.CoreAsync;
import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing manually resolving a {@code ResolvableFuture}.
 */
public class ManualResolvingExample {
  public static Stage<Integer> somethingReckless(final CoreAsync async) {
    final Completable<Integer> future = async.completable();

    // access the configured executor.
    async.executor().execute(() -> future.complete(42));

    return future;
  }

  public static void main(String[] argv) throws InterruptedException, ExecutionException {
    final CoreAsync async = Helpers.setup();

    System.out.println(somethingReckless(async).join());
  }
}
