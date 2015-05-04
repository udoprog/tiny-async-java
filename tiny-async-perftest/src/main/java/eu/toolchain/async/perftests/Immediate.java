package eu.toolchain.async.perftests;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.TestCase;
import eu.toolchain.async.TinyAsync;

public class Immediate {
    private static final int MANY_TRANSFORM_ITERATIONS = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    public static class Tiny implements TestCase {
        @Override
        public void test() throws Exception {
            final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < MANY_TRANSFORM_ITERATIONS; i++) {
                futures.add(async.resolved(true).transform(new LazyTransform<Boolean, Boolean>() {
                    @Override
                    public AsyncFuture<Boolean> transform(Boolean result) throws Exception {
                        return async.resolved(!result);
                    }
                }));
            }

            async.collect(futures).get();
        }
    }

    public static class Guava implements TestCase {
        @Override
        public void test() throws Exception {
            final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < MANY_TRANSFORM_ITERATIONS; i++) {
                futures.add(Futures.transform(Futures.immediateFuture(true), new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean input) {
                        return !input;
                    }
                }));
            }

            Futures.allAsList(futures).get();
        }
    }
}