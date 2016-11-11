package eu.toolchain.perftests.jmh;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureFramework;
import eu.toolchain.concurrent.TinyFuture;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;

public class Immediate {
  private static final int ITERATIONS = 10000;

  private static FutureFramework async = TinyFuture.builder().build();

  @Benchmark
  public void tiny() throws Exception {
    final List<CompletionStage<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < ITERATIONS; i++) {
      futures.add(async.completed(true).thenApply(result -> !result));
    }

    async.collect(futures).join();
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
