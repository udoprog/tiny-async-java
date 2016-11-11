package eu.toolchain.examples;

import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.TinyFuture;
import java.util.function.Function;

/**
 * An example application showcasing transforms.
 */
public class AsyncTransformExample {
  public static void main(String[] argv) throws Exception {
    TinyFuture async = FutureSetup.setup();

    final Function<Integer, Integer> addTen = i -> i + 10;

    final CompletionStage<Integer> f = async.call(() -> 10);

    System.out.println("result: " + f.thenApply(addTen).join());
  }
}
