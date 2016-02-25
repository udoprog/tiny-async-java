package eu.toolchain.examples;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * An example application showcasing static futures.
 */
public class AsyncStaticResultsExample {
    public static AsyncFuture<Integer> failToSetupFuture() {
        throw new IllegalStateException("i will never succeed");
    }

    public static AsyncFuture<Integer> failingOperation(TinyAsync async) {
        try {
            return failToSetupFuture();
        } catch (Exception e) {
            return async.failed(e);
        }
    }

    public static AsyncFuture<String> cachingOperation(TinyAsync async, boolean useCached) {
        // no need to perform expensive operation.
        // return a static value.
        if (useCached) {
            return async.resolved("cached");
        }

        return async.call(new Callable<String>() {
            @Override
            public String call() {
                return "deferred";
            }
        });
    }

    public static void main(String[] argv) throws Exception {
        TinyAsync async = AsyncSetup.setup();

        System.out.println("result(deferred): " + cachingOperation(async, false).get());
        System.out.println("result(cached): " + cachingOperation(async, true).get());

        try {
            failingOperation(async).get();
        } catch (ExecutionException e) {
            System.out.println("failingOperation: " + e);
        }
    }
}
