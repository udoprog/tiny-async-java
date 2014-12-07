package eu.toolchain.examples;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;

/**
 * An example application that showcases subscription of events on an {@code AsyncFuture}.
 */
public class AsyncBlockingExample {
    public static void main(String argv[]) throws InterruptedException, ExecutionException {
        TinyAsync async = AsyncSetup.setup();

        final AsyncFuture<Integer> f = async.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10;
            }
        });

        System.out.println("result: " + f.get());
    }
}
