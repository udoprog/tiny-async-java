package eu.toolchain.concurrent;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class HighLevelIT {
  private Async async;

  @Before
  public void setup() {
    async = CoreAsync.builder().executor(ForkJoinPool.commonPool()).build();
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

  @Test
  public void testComposeCancelForwarding() {
    final CompletableFuture<Integer> outer = async.future();
    final CompletableFuture<Integer> inner = async.future();

    final CompletionStage<Integer> third = outer.thenCompose(v -> inner);

    outer.complete(42);
    third.cancel();

    assertTrue(inner.isCancelled());
  }

  @Test
  public void testApply() throws Exception {
    final CompletableFuture<Integer> a = async.future();
    final CompletionStage<Integer> b = a.thenApply(v -> v + 10);

    a.complete(10);
    assertThat(b.join(), is(20));
  }
}
