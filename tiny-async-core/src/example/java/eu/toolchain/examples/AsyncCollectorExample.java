package eu.toolchain.examples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Collector;
import eu.toolchain.async.TinyAsync;

/**
 * An example application showcasing using a {@code Collector}.
 *
 * Will answer: what the sum is of all numbers from 1, to 100 (exclusive) in the most inneficient manner conceived.
 */
public class AsyncCollectorExample {
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

        final AsyncFuture<Integer> sum = async.collect(futures, new Collector<Integer, Integer>() {
            @Override
            public Integer collect(Collection<Integer> results) {
                int result = 0;

                for (Integer r : results)
                    result += r;

                return result;
            }
        });

        System.out.println("result: " + sum.get());
    }
}
