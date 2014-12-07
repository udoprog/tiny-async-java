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
    private final StreamCollector<S, T> reducable;
    private final ResolvableFuture<T> target;
    private final AtomicInteger countdown;

    private final AtomicInteger successful = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();

    public FutureStreamCollector(final AsyncCaller caller, final int size, final StreamCollector<S, T> reducable,
            final ResolvableFuture<T> target) {
        this.caller = caller;
        this.reducable = reducable;
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
        caller.failStreamCollector(reducable, error);
    }

    private void handleFinish(S result) {
        caller.resolveStreamCollector(reducable, result);
    }

    private void handleCancelled() {
        caller.cancelStreamCollector(reducable);
    }

    private void done() {
        final T result;

        try {
            result = reducable.end(successful.get(), failed.get(), cancelled.get());
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