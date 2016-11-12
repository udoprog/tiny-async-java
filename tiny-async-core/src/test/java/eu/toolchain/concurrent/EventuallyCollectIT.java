package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A high-level integration test for {@code TinyFuture#eventuallyCollect(java.util.Collection,
 * StreamCollector, int)}.
 */
public class EventuallyCollectIT {
  private ExecutorService executor;
  private ExecutorService otherExecutor;
  private FutureFramework async;
  private AtomicLong internalErrors;

  private static final long COUNT = 1000;
  private static final long EXPECTED_SUM = COUNT;
  private static final int PARALLELISM = 4;

  private static final long TIMEOUT = 5000;

  @Before
  public void setup() {
    executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    otherExecutor = Executors.newFixedThreadPool(10);

    internalErrors = new AtomicLong();

    async = TinyFuture.builder().executor(executor).caller(new DirectFutureCaller() {
      @Override
      protected void internalError(String what, Throwable e) {
        internalErrors.incrementAndGet();
      }
    }).build();
  }

  @After
  public void teardown() throws InterruptedException {
    executor.shutdown();
    executor.awaitTermination(100, TimeUnit.MILLISECONDS);

    otherExecutor.shutdown();
    otherExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
  }

  @Test(timeout = TIMEOUT)
  public void testBasic() throws Exception {
    int attempt = 0;

    while (attempt++ < 10) {
      final List<Callable<CompletionStage<Long>>> callables = new ArrayList<>();
      final AtomicInteger pending = new AtomicInteger();
      final AtomicInteger called = new AtomicInteger();

      for (long i = 0; i < COUNT; i++) {
        callables.add(() -> {
          pending.incrementAndGet();

          return async.call(() -> {
            if (pending.decrementAndGet() >= PARALLELISM) {
              throw new IllegalStateException("bad stuff");
            }

            called.incrementAndGet();
            return 1l;
          }, otherExecutor);
        });
      }

      final CompletionStage<Long> res =
          async.eventuallyCollect(callables, new StreamCollector<Long, Long>() {
            final AtomicLong sum = new AtomicLong();

            @Override
            public void completed(Long result) throws Exception {
              sum.addAndGet(result);
            }

            @Override
            public void failed(Throwable cause) throws Exception {
            }

            @Override
            public void cancelled() throws Exception {
            }

            @Override
            public Long end(int resolved, int failed, int cancelled) throws Exception {
              return sum.get();
            }
          }, PARALLELISM);

      assertEquals(EXPECTED_SUM, (long) res.join());
      assertEquals(COUNT, called.get());
    }
  }

  @Test(timeout = TIMEOUT)
  public void testRandomFailuresAndCancel() throws InterruptedException, ExecutionException {
    int attempt = 0;

    final Random r = new Random(0xffaa0000);

    final AtomicLong expectedInternalErrors = new AtomicLong();

    while (attempt++ < 10) {
      final List<Callable<CompletionStage<Long>>> callables = new ArrayList<>();
      final AtomicInteger pending = new AtomicInteger();

      for (long i = 0; i < COUNT; i++) {
        callables.add(() -> {
          pending.incrementAndGet();

          final CompletionStage<Long> f = async.call(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
              if (r.nextInt(2) == 1) {
                throw new RuntimeException("failure");
              }

              return 1l;
            }
          }, otherExecutor);

          if (r.nextInt(2) == 1) {
            f.cancel();
          }

          if (r.nextInt(2) == 1) {
            throw new RuntimeException("die");
          }

          return f;
        });
      }

      final CompletionStage<Long> res =
          async.eventuallyCollect(callables, new StreamCollector<Long, Long>() {
            @Override
            public void completed(Long result) throws Exception {
              if (r.nextInt(2) == 1) {
                expectedInternalErrors.incrementAndGet();
                throw new RuntimeException("die");
              }
            }

            @Override
            public void failed(Throwable cause) throws Exception {
              if (r.nextInt(2) == 1) {
                expectedInternalErrors.incrementAndGet();
                throw new RuntimeException("die");
              }
            }

            @Override
            public void cancelled() throws Exception {
              if (r.nextInt(2) == 1) {
                expectedInternalErrors.incrementAndGet();
                throw new RuntimeException("die");
              }
            }

            @Override
            public Long end(int resolved, int failed, int cancelled) throws Exception {
              return (long) (resolved + failed + cancelled);
            }
          }, PARALLELISM);

      assertEquals(COUNT, (long) res.join());
      assertEquals(expectedInternalErrors.get(), internalErrors.get());
    }
  }
}
