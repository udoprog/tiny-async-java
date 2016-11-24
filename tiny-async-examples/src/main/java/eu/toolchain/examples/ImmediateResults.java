package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.examples.helpers.Helpers;
import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing static futures.
 */
public class ImmediateResults {
  public static Stage<Integer> failToSetupFuture() {
    throw new IllegalStateException("i will never succeed");
  }

  public static Stage<Integer> failingOperation(final Async async) {
    try {
      return failToSetupFuture();
    } catch (Exception e) {
      return async.failed(e);
    }
  }

  public static Stage<String> cachingOperation(final Async async, boolean useCached) {
    // no need to perform expensive operation.
    // return a static value.
    if (useCached) {
      return async.completed("cached");
    }

    return async.call(() -> "deferred");
  }

  public static void main(String[] argv) throws Exception {
    final Async async = Helpers.setup();

    System.out.println("result(deferred): " + cachingOperation(async, false).join());
    System.out.println("result(cached): " + cachingOperation(async, true).join());

    try {
      failingOperation(async).join();
    } catch (final ExecutionException e) {
      System.out.println("failingOperation: " + e);
    }
  }
}
