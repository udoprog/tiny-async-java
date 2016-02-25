package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinator thread for handling delayed callables executing with a given parallelism.
 *
 * @param <S> The source type being collected.
 * @param <T> The target type the source type is being collected into.
 */
public class DelayedCollectCoordinator<S, T> implements FutureDone<S>, Runnable {
    private final AtomicInteger pending = new AtomicInteger();

    private final AtomicInteger cancelled = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    /* lock that must be acquired before using {@link callables} */
    private final Object lock = new Object();

    private final AsyncCaller caller;
    private final Iterator<? extends Callable<? extends AsyncFuture<? extends S>>> callables;
    private final StreamCollector<? super S, ? extends T> collector;
    private final ResolvableFuture<? super T> future;
    private final int parallelism;
    private final int total;

    volatile boolean cancel = false;
    volatile boolean done = false;

    public DelayedCollectCoordinator(
        final AsyncCaller caller,
        final Collection<? extends Callable<? extends AsyncFuture<? extends S>>> callables,
        final StreamCollector<S, T> collector, final ResolvableFuture<? super T> future,
        int parallelism
    ) {
        this.caller = caller;
        this.callables = callables.iterator();
        this.collector = collector;
        this.future = future;
        this.parallelism = parallelism;
        this.total = callables.size();
    }

    @Override
    public void failed(Throwable cause) {
        caller.fail(collector, cause);
        pending.decrementAndGet();
        failed.incrementAndGet();
        cancel = true;
        checkNext();
    }

    @Override
    public void resolved(S result) {
        caller.resolve(collector, result);
        pending.decrementAndGet();
        checkNext();
    }

    @Override
    public void cancelled() {
        caller.cancel(collector);
        pending.decrementAndGet();
        cancelled.incrementAndGet();
        cancel = true;
        checkNext();
    }

    // coordinate thread.
    @Override
    public void run() {
        synchronized (lock) {
            if (!callables.hasNext()) {
                checkEnd();
                return;
            }

            for (int i = 0; i < parallelism && callables.hasNext(); i++) {
                setupNext(callables.next());
            }
        }

        future.onCancelled(new FutureCancelled() {
            @Override
            public void cancelled() throws Exception {
                cancel = true;
                checkNext();
            }
        });
    }

    private void checkNext() {
        final Callable<? extends AsyncFuture<? extends S>> next;

        synchronized (lock) {
            // cancel any available callbacks.
            if (cancel) {
                while (callables.hasNext()) {
                    callables.next();
                    caller.cancel(collector);
                }
            }

            if (!callables.hasNext()) {
                checkEnd();
                return;
            }

            next = callables.next();
        }

        setupNext(next);
    }

    private void setupNext(final Callable<? extends AsyncFuture<? extends S>> next) {
        final AsyncFuture<? extends S> f;

        pending.incrementAndGet();

        try {
            f = next.call();
        } catch (final Exception e) {
            failed(e);
            return;
        }

        f.onDone(this);
    }

    private void checkEnd() {
        if (pending.get() > 0) {
            return;
        }

        if (done) {
            return;
        }

        done = true;

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
