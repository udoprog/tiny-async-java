package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinator thread for handling delayed callables executing with a given parallelism.
 * 
 * @param <S> The source type being collected.
 * @param <T> The target type the source type is being collected into.
 */
public class DelayedCollectCoordinator<S, T> implements FutureDone<S>, Runnable {
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();

    private final AsyncCaller caller;
    private final Collection<? extends Callable<? extends AsyncFuture<? extends S>>> callables;
    private final StreamCollector<? super S, ? extends T> collector;
    private final Semaphore mutex;
    private final ResolvableFuture<? super T> future;
    private final int totalPermitsToAcquire;

    final AtomicBoolean cancel = new AtomicBoolean();

    public DelayedCollectCoordinator(final AsyncCaller caller,
            final Collection<? extends Callable<? extends AsyncFuture<? extends S>>> callables,
            final StreamCollector<S, T> collector, final Semaphore mutex, final ResolvableFuture<? super T> future,
            int parallelism) {
        this.caller = caller;
        this.callables = callables;
        this.collector = collector;
        this.mutex = mutex;
        this.future = future;
        this.totalPermitsToAcquire = (callables.size() + parallelism);
    }

    @Override
    public void failed(Throwable cause) {
        caller.fail(collector, cause);
        failed.incrementAndGet();
        cancel.set(true);
        mutex.release();
    }

    @Override
    public void resolved(S result) {
        caller.resolve(collector, result);
        mutex.release();
    }

    @Override
    public void cancelled() {
        caller.cancel(collector);
        cancelled.incrementAndGet();
        cancel.set(true);
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
        final Iterator<? extends Callable<? extends AsyncFuture<? extends S>>> iterator = callables.iterator();

        int acquired = 0;

        while (iterator.hasNext() && !cancel.get()) {
            try {
                mutex.acquire();
            } catch (Exception e) {
                future.fail(e);
                return;
            }

            ++acquired;

            final Callable<? extends AsyncFuture<? extends S>> callable = iterator.next();

            final AsyncFuture<? extends S> f;

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
        while (acquired++ < totalPermitsToAcquire) {
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