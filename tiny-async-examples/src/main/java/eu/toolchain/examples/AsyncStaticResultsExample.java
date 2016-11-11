package eu.toolchain.examples;

import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.TinyFuture;
import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing static futures.
 */
public class AsyncStaticResultsExample {
  public static CompletionStage<Integer> failToSetupFuture() {
    throw new IllegalStateException("i will never succeed");
  }

  public static CompletionStage<Integer> failingOperation(TinyFuture async) {
    try {
      return failToSetupFuture();
    } catch (Exception e) {
      return async.failed(e);
    }
  }

  public static CompletionStage<String> cachingOperation(TinyFuture async, boolean useCached) {
    // no need to perform expensive operation.
    // return a static value.
    if (useCached) {
      return async.completed("cached");
    }

    return async.call(() -> "deferred");
  }

  public static void main(String[] argv) throws Exception {
    TinyFuture async = FutureSetup.setup();

    System.out.println("result(deferred): " + cachingOperation(async, false).join());
    System.out.println("result(cached): " + cachingOperation(async, true).join());

    try {
      failingOperation(async).join();
    } catch (final ExecutionException e) {
      System.out.println("failingOperation: " + e);
    }
  }
}
