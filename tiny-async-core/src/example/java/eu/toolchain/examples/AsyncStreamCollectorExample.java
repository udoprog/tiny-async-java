package eu.toolchain.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.StreamCollector;
import eu.toolchain.async.TinyAsync;

/**
 * An example application showcasing using a {@code StreamCollector}.
 */
public class AsyncStreamCollectorExample {
    public static void main(String[] argv) throws InterruptedException, ExecutionException {
        TinyAsync async = AsyncSetup.setup();

        final List<AsyncFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final int value = i;

            futures.add(async.call(new Callable<Integer>() {
                @Override
                public Integer call() {
                    return value;
                }
            }));
        }

        final AsyncFuture<Integer> sum = async.collect(futures, new StreamCollector<Integer, Integer>() {
            private final AtomicInteger result = new AtomicInteger();

            @Override
            public void resolved(Integer result) throws Exception {
                this.result.addAndGet(result);
            }

            @Override
            public void failed(Throwable cause) throws Exception {
            }

            @Override
            public void cancelled() throws Exception {
            }

            @Override
            public Integer end(int resolved, int failed, int cancelled) throws Exception {
                return this.result.get();
            }
        });

        System.out.println("result: " + sum.get());
    }
}
