package eu.toolchain.perftests.jmh;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;

public class CollectMany {
    private static final int SIZE = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    @Benchmark
    public void tiny() throws Exception {
        final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < SIZE; i++)
            futures.add(async.resolved(true));

        async.collect(futures).get();
    }

    @Benchmark
    public void guava() throws Exception {
        final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < SIZE; i++)
            futures.add(Futures.immediateFuture(true));

        Futures.allAsList(futures).get();
    }
}