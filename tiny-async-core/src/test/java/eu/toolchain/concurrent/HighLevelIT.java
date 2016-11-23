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
    final Completable<Integer> a = async.completable();
    final Completable<Integer> b = async.completable();
    final Completable<Integer> c = async.completable();

    final Stage<Void> f = async.collectAndDiscard(Stream.of(a, b, c));

    a.complete(42);
    f.cancel();

    assertFalse(a.isCancelled());
    assertTrue(b.isCancelled());
    assertTrue(c.isCancelled());
  }

  @Test
  public void testComposeCancelForwarding() {
    final Completable<Integer> outer = async.completable();
    final Completable<Integer> inner = async.completable();

    final Stage<Integer> third = outer.thenCompose(v -> inner);

    outer.complete(42);
    third.cancel();

    assertTrue(inner.isCancelled());
  }

  @Test
  public void testApplyCancelForwarding() {
    final Completable<Integer> outer = async.completable();
    final Stage<Integer> second = outer.thenApply(v -> v + 1);

    second.cancel();

    assertTrue(outer.isCancelled());
  }

  @Test
  public void testApply() throws Exception {
    final Completable<Integer> a = async.completable();
    final Stage<Integer> b = a.thenApply(v -> v + 10);

    a.complete(10);
    assertThat(b.join(), is(20));
  }
}
