package eu.toolchain.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TinyAsyncReloadableManagedIntegrationTest {
    final Object REF = new Object();

    private static final long TIMEOUT = 20000;

    // setup an direct async framework.
    final AsyncFramework async = TinyAsync.builder().threaded(false).build();

    private AtomicInteger start;
    private AtomicInteger stop;

    private ReloadableManaged<Object> managed;

    @Before
    public void setup() {
        start = new AtomicInteger();
        stop = new AtomicInteger();

        managed = async.reloadableManaged(new ManagedSetup<Object>() {
            @Override
            public AsyncFuture<Object> construct() {
                start.incrementAndGet();
                return async.resolved(REF);
            }

            @Override
            public AsyncFuture<Void> destruct(Object value) {
                stop.incrementAndGet();
                return async.resolved(null);
            }
        });
    }

    @Test(timeout = TIMEOUT)
    public void testReloadOnce() throws Exception {
        managed.start().get();
        managed.start().get();

        Assert.assertEquals(1, start.get());
        Assert.assertEquals(0, stop.get());

        managed.reload(true).get();

        Assert.assertEquals(2, start.get());
        Assert.assertEquals(1, stop.get());
    }

    @Test(timeout = TIMEOUT)
    public void testReload() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);

        final AtomicLong valid = new AtomicLong();
        final AtomicLong invalid = new AtomicLong();

        final int size = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        final CountDownLatch latch = new CountDownLatch(size);

        final List<Thread> threads = new ArrayList<>();

        managed.start().get();

        for (int i = 0; i < size; i++) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running.get()) {
                        final Borrowed<Object> b;

                        if ((b = managed.borrow()).isValid()) {
                            valid.incrementAndGet();
                        } else {
                            invalid.incrementAndGet();
                        }

                        b.release();
                    }

                    latch.countDown();
                }
            });

            threads.add(t);
            t.start();
        }

        for (int i = 0; i < 1000; i++) {
            System.out.println("Reload #" + (i + 1));
            managed.reload(true).get();
            Thread.sleep(1);
        }

        running.set(false);

        latch.await();

        for (final Thread t : threads) {
            t.join();
        }

        managed.stop().get();

        System.out.println("Valid: " + valid.get());
        System.out.println("Invalid: " + invalid.get());

        Assert.assertEquals(0l, invalid.get());
    }
}
