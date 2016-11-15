package eu.toolchain.perftests.jmh;

import com.google.common.util.concurrent.ListenableFuture;
import eu.toolchain.concurrent.Stage;
import eu.toolchain.concurrent.CoreAsync;
import eu.toolchain.concurrent.Async;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;

public class CollectFew {
  private static final int SIZE = 10;

  private static Async async = CoreAsync.builder().build();

  @Benchmark
  public void tiny() throws Exception {
    final List<Stage<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < SIZE; i++) {
      futures.add(async.completed(true));
    }

    async.collect(futures).join();
  }

  @Benchmark
  public void guava() throws Exception {
    final List<ListenableFuture<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < SIZE; i++) {
      futures.add(com.google.common.util.concurrent.Futures.immediateFuture(true));
    }

    com.google.common.util.concurrent.Futures.allAsList(futures).get();
  }
}
