package eu.toolchain.async.collector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.StreamCollector;

/**
 * Implementation of {@link AsyncFuture#end(List, AsyncFuture.StreamCollector)}.
 *
 * @author udoprog
 *
 * @param <T>
 */
public class FutureStreamCollector<S, T> implements FutureDone<S> {
    private final AsyncCaller caller;
    private final StreamCollector<S, T> collector;
    private final ResolvableFuture<? super T> target;
    private final AtomicInteger countdown;

    private final AtomicInteger successful = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();

    public FutureStreamCollector(final AsyncCaller caller, final int size, final StreamCollector<S, T> collector,
            final ResolvableFuture<? super T> target) {
        this.caller = caller;
        this.collector = collector;
        this.target = target;
        this.countdown = new AtomicInteger(size);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        failed.incrementAndGet();
        handleError(e);
        check();
    }

    @Override
    public void resolved(S result) throws Exception {
        successful.incrementAndGet();
        handleFinish(result);
        check();
    }

    @Override
    public void cancelled() throws Exception {
        cancelled.incrementAndGet();
        handleCancelled();
        check();
    }

    private void handleError(Throwable error) {
        caller.failStreamCollector(collector, error);
    }

    private void handleFinish(S result) {
        caller.resolveStreamCollector(collector, result);
    }

    private void handleCancelled() {
        caller.cancelStreamCollector(collector);
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

    private void check() throws Exception {
        if (countdown.decrementAndGet() == 0)
            done();
    }
}