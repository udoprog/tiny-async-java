package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Completable;
import eu.toolchain.examples.helpers.Helpers;
import java.io.PrintStream;

/**
 * Shows how to build different kinds of stages and completables from scratch.
 */
public class FromScratch {
  public static void main(String[] argv) {
    final PrintStream out = System.out;
    final Async async = Helpers.setup();

    final Completable<Integer> completable = async.completable();

    out.println("# Manual completion");
    out.println(completable);
    completable.complete(42);
    out.println(completable);

    out.println("# Immediate optimization");
    out.println(completable.thenApply(v -> v + 20));

    out.println("# Immediate values");
    out.println(async.completed());
    out.println(async.completed(42));
    out.println(async.cancelled());
    out.println(async.failed(new Exception("oh no")));
  }
}
