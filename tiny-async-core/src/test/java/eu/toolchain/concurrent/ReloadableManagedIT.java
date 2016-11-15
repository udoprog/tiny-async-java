package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class ReloadableManagedIT {
  final Object REF = new Object();

  @Rule
  public Timeout timeout = Timeout.millis(20000);

  // setup an direct async framework.
  final Async async = CoreAsync.builder().threaded(false).build();

  private AtomicInteger start;
  private AtomicInteger stop;

  private ReloadableManaged<Object> managed;

  @Before
  public void setup() {
    start = new AtomicInteger();
    stop = new AtomicInteger();

    managed = async.reloadableManaged(() -> {
      start.incrementAndGet();
      return async.completed(REF);
    }, value -> {
      stop.incrementAndGet();
      return async.completed();
    });
  }

  @Test
  public void testReloadOnce() throws Exception {
    managed.start().join();
    managed.start().join();

    assertEquals(1, start.get());
    assertEquals(0, stop.get());

    managed.reload().join();

    assertEquals(2, start.get());
    assertEquals(1, stop.get());
  }

  @Test
  public void testReload() throws Exception {
    final AtomicBoolean runHitters = new AtomicBoolean(true);

    final AtomicLong valid = new AtomicLong();
    final AtomicLong invalid = new AtomicLong();
    final AtomicLong errors = new AtomicLong();

    final int iterations = 10000;
    final int threadSize = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
    assert(threadSize >= 4 && (threadSize % 2) == 0);

    final int poolSize = threadSize - 2;

    final CountDownLatch hitters = new CountDownLatch(poolSize);
    final CountDownLatch reloading = new CountDownLatch(poolSize);

    final List<Thread> threads = new ArrayList<>();

    managed.start().join();

    // spawn a set of threads dead-set on borrowing this future
    for (int thread = 0; thread < poolSize; thread++) {
      final Thread t = new Thread(() -> {
        while (runHitters.get()) {
          final Borrowed<Object> b;

          if ((b = managed.borrow()).isValid()) {
            valid.incrementAndGet();
          } else {
            invalid.incrementAndGet();
          }

          b.release();
        }

        hitters.countDown();
      });

      threads.add(t);
      t.start();
    }

    for (int thread = 0; thread < poolSize; thread++) {
      final Thread t = new Thread(() -> {
        for (int i = 0; i < iterations; i++) {
          try {
            managed.reload().join();
          } catch (final Exception e) {
            e.printStackTrace();
            errors.incrementAndGet();
          }
        }

        reloading.countDown();
      });

      threads.add(t);
      t.start();
    }

    reloading.await();
    runHitters.set(false);
    hitters.await();

    for (final Thread t : threads) {
      t.join();
    }

    managed.stop().join();

    System.out.println("Start: " + start.get());
    System.out.println("Stop: " + stop.get());
    System.out.println("Valid: " + valid.get());
    System.out.println("Invalid: " + invalid.get());

    assertEquals(0L, invalid.get());
    assertEquals(0L, errors.get());
  }
}
