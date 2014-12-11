package eu.toolchain.async;

import java.util.ArrayList;
import java.util.List;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class TransformMany {
    private static final int IMMEDIATE_SIZE = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    public static class Tiny implements TestCase {
        @Override
        public void test() throws Exception {
            final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < IMMEDIATE_SIZE; i++) {
                futures.add(async.resolved(true));
            }

            async.collect(futures).get();
        }
    }

    public static class Guava implements TestCase {
        @Override
        public void test() throws Exception {
            final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < IMMEDIATE_SIZE; i++) {
                futures.add(Futures.immediateFuture(true));
            }

            Futures.allAsList(futures).get();
        }
    }
}