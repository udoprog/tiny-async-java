package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinator thread for handling delayed callables executing with a given parallelism.
 */
public class DelayedCollectCoordinator<C, T> implements FutureDone<C>, Runnable {
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();

    private final AsyncCaller caller;
    private final Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables;
    private final StreamCollector<? super C, ? extends T> collector;
    private final TinySemaphore mutex;
    private final ResolvableFuture<? super T> future;

    final AtomicBoolean cancel = new AtomicBoolean();

    public DelayedCollectCoordinator(final AsyncCaller caller,
            final Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables,
            final StreamCollector<C, T> collector, final TinySemaphore mutex, final ResolvableFuture<? super T> future) {
        this.caller = caller;
        this.callables = callables;
        this.collector = collector;
        this.mutex = mutex;
        this.future = future;
    }

    @Override
    public void failed(Throwable cause) {
        failed.incrementAndGet();
        cancel.set(true);
        caller.failStreamCollector(collector, cause);
        mutex.release();
    }

    @Override
    public void resolved(C result) {
        caller.resolveStreamCollector(collector, result);
        mutex.release();
    }

    @Override
    public void cancelled() {
        cancelled.incrementAndGet();
        cancel.set(true);
        caller.cancelStreamCollector(collector);
        mutex.release();
    }

    // coordinate thread.
    @Override
    public void run() {
        future.on(new FutureCancelled() {
            @Override
            public void cancelled() throws Exception {
                cancel.set(true);
                mutex.release();
            }
        });

        final int total = callables.size();
        final Iterator<? extends Callable<? extends AsyncFuture<? extends C>>> iterator = callables.iterator();

        int acquired = 0;

        while (iterator.hasNext() && !cancel.get()) {
            try {
                mutex.acquire();
            } catch (Exception e) {
                future.fail(e);
                return;
            }

            ++acquired;

            final Callable<? extends AsyncFuture<? extends C>> callable = iterator.next();

            final AsyncFuture<? extends C> f;

            try {
                f = callable.call();
            } catch (final Exception e) {
                failed(e);
                break;
            }

            f.on(this);
        }

        // cleanup, cancel all future callbacks.
        while (iterator.hasNext()) {
            iterator.next();
            cancelled();
        }

        // wait for the rest of the pending futures...
        while (acquired++ < total) {
            try {
                mutex.acquire();
            } catch (Exception e) {
                future.fail(e);
                return;
            }
        }

        final int f = failed.get();
        final int c = cancelled.get();
        final int r = total - f - c;

        final T value;

        try {
            value = collector.end(r, f, c);
        } catch (Exception e) {
            future.fail(e);
            return;
        }

        future.resolve(value);
    }
}