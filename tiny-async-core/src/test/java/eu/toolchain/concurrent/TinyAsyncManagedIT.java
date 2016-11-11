package eu.toolchain.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class TinyAsyncManagedIT {
  final Object REF = new Object();

  // setup an direct async framework.
  final FutureFramework async = TinyFuture.builder().build();

  private AtomicInteger start;
  private AtomicInteger stop;

  private Managed<Object> managed;

  @Rule
  public Timeout timeout = Timeout.millis(1000);

  @Before
  public void setup() {
    start = new AtomicInteger();
    stop = new AtomicInteger();

    managed = async.managed(() -> {
      start.incrementAndGet();
      return async.completed(REF);
    }, value -> {
      stop.incrementAndGet();
      return async.completed();
    });
  }

  @Test
  public void testStartOnce() {
    managed.start();
    managed.start();

    Assert.assertEquals(1, start.get());
  }

  @Test
  public void testBorrow() throws Exception {
    managed.start().join();

    Assert.assertEquals(1, start.get());

    final AtomicInteger finished = new AtomicInteger();

    try (final Borrowed<Object> b = managed.borrow()) {
      managed.stop().whenFinished(finished::incrementAndGet);

      Assert.assertTrue("should timeout", doesStopTimeout());
      Assert.assertEquals(0, finished.get());
      Assert.assertEquals(0, stop.get());
    }

    Assert.assertFalse("should not timeout", doesStopTimeout());
    Assert.assertEquals(1, finished.get());
    Assert.assertEquals(1, stop.get());
  }

  @Test
  public void testInvalidFutureAfterStop() throws Exception {
    managed.start().join();
    managed.stop().join();

    Assert.assertEquals(1, start.get());
    Assert.assertEquals(1, stop.get());

    final AtomicInteger finished = new AtomicInteger();

    try (final Borrowed<Object> b = managed.borrow()) {
      Assert.assertEquals(false, b.isValid());

      managed.stop().whenFinished(finished::incrementAndGet);

      Assert.assertFalse("should not timeout", doesStopTimeout());
      Assert.assertEquals(1, finished.get());
      Assert.assertEquals(1, stop.get());
    }

    Assert.assertEquals(1, finished.get());
    Assert.assertEquals(1, stop.get());
  }

  private boolean doesStopTimeout() throws Exception {
    try {
      managed.stop().join(10, TimeUnit.MILLISECONDS);
      return false;
    } catch (final TimeoutException e) {
      return true;
    }
  }
}
