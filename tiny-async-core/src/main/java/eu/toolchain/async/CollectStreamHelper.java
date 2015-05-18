package eu.toolchain.async;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link AsyncFramework#collect(Collection, StreamCollector)}.
 *
 * @author udoprog
 *
 * @param <S> the source type being collected.
 * @param <T> The type the source type is being collected and transformed into.
 */
public class CollectStreamHelper<S, T> implements FutureDone<S> {
    private final AsyncCaller caller;
    private final StreamCollector<S, T> collector;
    private final ResolvableFuture<? super T> target;
    private final AtomicInteger countdown;

    private final AtomicInteger successful = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();

    public CollectStreamHelper(final AsyncCaller caller, final int size, final StreamCollector<S, T> collector,
            final ResolvableFuture<? super T> target) {
        if (size <= 0)
            throw new IllegalArgumentException("size");

        this.caller = caller;
        this.collector = collector;
        this.target = target;
        this.countdown = new AtomicInteger(size);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        failed.incrementAndGet();
        caller.fail(collector, e);
        check();
    }

    @Override
    public void resolved(S result) throws Exception {
        successful.incrementAndGet();
        caller.resolve(collector, result);
        check();
    }

    @Override
    public void cancelled() throws Exception {
        cancelled.incrementAndGet();
        caller.cancel(collector);
        check();
    }

    private void check() throws Exception {
        if (countdown.decrementAndGet() == 0)
            done();
    }

    private void done() {
        final T result;

        try {
            result = collector.end(successful.get(), failed.get(), cancelled.get());
        } catch (Exception e) {
            target.fail(e);
            return;
        }

        target.resolve(result);
    }
}