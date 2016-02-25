package eu.toolchain.examples;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.TinyAsync;

import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing manually resolving a {@code ResolvableFuture}.
 */
public class AsyncManualResolvingExample {
    public static AsyncFuture<Integer> somethingReckless(TinyAsync async) {
        final ResolvableFuture<Integer> future = async.future();

        // access the configured executor.
        async.defaultExecutor().execute(new Runnable() {
            @Override
            public void run() {
                future.resolve(42);
            }
        });

        return future;
    }

    public static void main(String[] argv) throws InterruptedException, ExecutionException {
        TinyAsync async = AsyncSetup.setup();

        System.out.println(somethingReckless(async).get());
    }
}
