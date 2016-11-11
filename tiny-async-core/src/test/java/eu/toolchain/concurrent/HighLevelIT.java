package eu.toolchain.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class HighLevelIT {
  private FutureFramework async;

  @Before
  public void setup() {
    async = TinyFuture.builder().executor(ForkJoinPool.commonPool()).build();
  }

  @Test
  public void testCollectCancelForwarding() {
    final CompletableFuture<Integer> a = async.future();
    final CompletableFuture<Integer> b = async.future();
    final CompletableFuture<Integer> c = async.future();

    final CompletionStage<Void> f = async.collectAndDiscard(Stream.of(a, b, c));

    a.complete(42);
    f.cancel();

    assertFalse(a.isCancelled());
    assertTrue(b.isCancelled());
    assertTrue(c.isCancelled());
  }
}
