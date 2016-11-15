package eu.toolchain.perftests.jmh;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.concurrent.CoreAsync;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;

public class Immediate {
  private static final int ITERATIONS = 10000;

  private static Async async = CoreAsync.builder().build();

  @Benchmark
  public void tiny() throws Exception {
    final List<Stage<Boolean>> futures = new ArrayList<>();

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
          com.google.common.util.concurrent.Futures.transform(
              com.google.common.util.concurrent.Futures.immediateFuture(true), new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean input) {
              return !input;
            }
          }));
    }

    com.google.common.util.concurrent.Futures.allAsList(futures).get();
  }
}
