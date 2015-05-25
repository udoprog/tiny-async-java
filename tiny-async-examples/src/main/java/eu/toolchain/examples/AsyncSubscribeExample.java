package eu.toolchain.examples;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.TinyAsync;

/**
 * An example application that showcases subscription of events on an {@code AsyncFuture}.
 */
public class AsyncSubscribeExample {
    public static void main(String argv[]) throws InterruptedException, ExecutionException {
        TinyAsync async = AsyncSetup.setup();

        final AsyncFuture<Integer> f = async.call(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 10;
            }
        });

        f.on(new FutureDone<Integer>() {
            @Override
            public void resolved(Integer result) throws Exception {
                System.out.println("result: " + result);
            }

            // uh-oh. Something went wrong.
            @Override
            public void failed(Throwable e) throws Exception {
                System.out.println("error: " + e);
            }

            @Override
            public void cancelled() throws Exception {
                System.out.println("cancelled");
            }
        });

        System.out.println("result: " + f.get());
    }
}
