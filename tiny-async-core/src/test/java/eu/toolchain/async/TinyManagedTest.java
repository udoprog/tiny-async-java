package eu.toolchain.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TinyManagedTest {
    final Object REF = new Object();

    private static final long TIMEOUT = 200;

    // setup an direct async framework.
    final AsyncFramework async = TinyAsync.builder().threaded(false).build();

    private AtomicInteger start;
    private AtomicInteger stop;

    private Managed<Object> managed;

    @Before
    public void setup() {
        start = new AtomicInteger();
        stop = new AtomicInteger();

        managed = async.managed(new ManagedSetup<Object>() {
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

    @Test
    public void testStartOnce() {
        managed.start();
        managed.start();

        Assert.assertEquals(1, start.get());
    }

    @Test(timeout = TIMEOUT)
    public void testBorrow() throws Exception {
        managed.start().get();

        Assert.assertEquals(1, start.get());

        final AtomicInteger finished = new AtomicInteger();

        try (final Borrowed<Object> b = managed.borrow()) {
            managed.stop().on(new FutureFinished() {
                @Override
                public void finished() throws Exception {
                    finished.incrementAndGet();
                }
            });

            Assert.assertTrue("should timeout", doesStopTimeout());
            Assert.assertEquals(0, finished.get());
            Assert.assertEquals(0, stop.get());
        }

        Assert.assertFalse("should not timeout", doesStopTimeout());
        Assert.assertEquals(1, finished.get());
        Assert.assertEquals(1, stop.get());
    }

    @Test(timeout = TIMEOUT)
    public void testInvalidFutureAfterStop() throws Exception {
        managed.start().get();
        managed.stop().get();

        Assert.assertEquals(1, start.get());
        Assert.assertEquals(1, stop.get());

        final AtomicInteger finished = new AtomicInteger();

        try (final Borrowed<Object> b = managed.borrow()) {
            Assert.assertEquals(false, b.isValid());

            managed.stop().on(new FutureFinished() {
                @Override
                public void finished() throws Exception {
                    finished.incrementAndGet();
                }
            });

            Assert.assertFalse("should not timeout", doesStopTimeout());
            Assert.assertEquals(1, finished.get());
            Assert.assertEquals(1, stop.get());
        }

        Assert.assertEquals(1, finished.get());
        Assert.assertEquals(1, stop.get());
    }

    private boolean doesStopTimeout() throws Exception {
        try {
            managed.stop().get(10, TimeUnit.MILLISECONDS);
            return false;
        } catch (TimeoutException e) {
            return true;
        }
    }
}