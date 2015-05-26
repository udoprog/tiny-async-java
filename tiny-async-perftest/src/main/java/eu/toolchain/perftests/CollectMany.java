package eu.toolchain.perftests;

import java.util.ArrayList;
import java.util.List;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;

public class CollectMany implements TestCase {
    private static final int IMMEDIATE_SIZE = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    @Override
    public void tiny() throws Exception {
        final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < IMMEDIATE_SIZE; i++) {
            futures.add(async.resolved(true));
        }

        async.collect(futures).get();
    }

    @Override
    public void guava() throws Exception {
        final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < IMMEDIATE_SIZE; i++) {
            futures.add(Futures.immediateFuture(true));
        }

        Futures.allAsList(futures).get();
    }
}