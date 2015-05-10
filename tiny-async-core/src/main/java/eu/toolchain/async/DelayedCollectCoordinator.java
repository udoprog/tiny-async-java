package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinator thread for handling delayed callables executing with a given parallelism.
 */
public class DelayedCollectCoordinator<C, T> implements FutureDone<C>, Runnable {
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();
    private final AtomicBoolean cancel = new AtomicBoolean();

    private final AsyncCaller caller;
    private final Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables;
    private final StreamCollector<? super C, ? extends T> collector;
    private final Semaphore semaphore;
    private final ResolvableFuture<? super T> future;

    public DelayedCollectCoordinator(final AsyncCaller caller,
            final Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables,
            final StreamCollector<C, T> collector, final int parallelism, final ResolvableFuture<? super T> future) {
        this.caller = caller;
        this.callables = callables;
        this.collector = collector;
        this.semaphore = new Semaphore(parallelism);
        this.future = future;

        future.on(new FutureCancelled() {
            @Override
            public void cancelled() throws Exception {
                cancel.set(true);
                semaphore.release();
            }
        });
    }

    @Override
    public void failed(Throwable cause) throws Exception {
        caller.failStreamCollector(collector, cause);
        failed.incrementAndGet();
        semaphore.release();
    }

    @Override
    public void resolved(C result) throws Exception {
        caller.resolveStreamCollector(collector, result);
        semaphore.release();
    }

    @Override
    public void cancelled() throws Exception {
        caller.cancelStreamCollector(collector);
        cancelled.incrementAndGet();
        semaphore.release();
    }

    // coordinate thread.
    @Override
    public void run() {
        final int total = callables.size();
        final Iterator<? extends Callable<? extends AsyncFuture<? extends C>>> iterator = callables.iterator();

        int acquired = 0;

        while (iterator.hasNext()) {
            try {
                semaphore.acquire();
            } catch (Exception e) {
                future.fail(e);
                return;
            }

            if (cancel.get())
                break;

            ++acquired;

            if (failed.get() > 0)
                break;

            final Callable<? extends AsyncFuture<? extends C>> callable = iterator.next();

            final AsyncFuture<? extends C> f;

            try {
                f = callable.call();
            } catch (final Exception e) {
                caller.failFutureDone(this, e);
                break;
            }

            f.on(this);
        }

        // cleanup, cancel all future callbacks.
        while (iterator.hasNext()) {
            iterator.next();
            caller.cancelFutureDone(this);
        }

        // still some pending futures to take care of...
        while (acquired++ < total) {
            try {
                semaphore.acquire();
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