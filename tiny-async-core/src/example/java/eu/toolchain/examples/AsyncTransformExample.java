package eu.toolchain.examples;

import java.util.concurrent.Callable;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.Transform;

/**
 * An example application showcasing transforms.
 */
public class AsyncTransformExample {
    public static void main(String[] argv) throws Exception {
        TinyAsync async = AsyncSetup.setup();

        final Transform<Integer, Integer> addTen = new Transform<Integer, Integer>() {
            @Override
            public Integer transform(Integer i) {
                return i + 10;
            }
        };

        final AsyncFuture<Integer> f = async.call(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 10;
            }
        });

        System.out.println("result: " + f.transform(addTen).get());
    }
}
