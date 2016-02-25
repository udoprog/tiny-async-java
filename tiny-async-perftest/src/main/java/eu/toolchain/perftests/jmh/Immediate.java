package eu.toolchain.perftests.jmh;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.Transform;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.ArrayList;
import java.util.List;

public class Immediate {
    private static final int ITERATIONS = 10000;

    private static AsyncFramework async = TinyAsync.builder().build();

    @Benchmark
    public void tiny() throws Exception {
        final List<AsyncFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < ITERATIONS; i++) {
            futures.add(async.resolved(true).directTransform(new Transform<Boolean, Boolean>() {
                @Override
                public Boolean transform(Boolean result) throws Exception {
                    return !result;
                }
            }));
        }

        async.collect(futures).get();
    }

    @Benchmark
    public void guava() throws Exception {
        final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < ITERATIONS; i++) {
            futures.add(
                Futures.transform(Futures.immediateFuture(true), new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean input) {
                        return !input;
                    }
                }));
        }

        Futures.allAsList(futures).get();
    }
}
