package eu.toolchain.async;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class Immediate {
    private static final int MANY_TRANSFORM_ITERATIONS = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    public static class Tiny implements TestCase {
        @Override
        public void test() throws Exception {
            final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < MANY_TRANSFORM_ITERATIONS; i++) {
                futures.add(async.resolved(true).transform(new Transform<Boolean, Boolean>() {
                    @Override
                    public Boolean transform(Boolean result) throws Exception {
                        return !result;
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