package eu.toolchain.async.collector;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.ThrowableUtils;

/**
 * Implementation of {@link AsyncFuture#end(List, AsyncFuture.StreamCollector)}.
 *
 * @author udoprog
 *
 * @param <T>
 */
public class FutureDiscardCollector<S> implements FutureDone<S> {
    private final ResolvableFuture<Void> target;
    private final AtomicInteger countdown;
    private final AtomicInteger cancelled = new AtomicInteger();
    private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

    public FutureDiscardCollector(int size, ResolvableFuture<Void> target) {
        this.target = target;
        this.countdown = new AtomicInteger(size);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        errors.add(e);
        check();
    }

    @Override
    public void resolved(S result) throws Exception {
        check();
    }

    @Override
    public void cancelled() throws Exception {
        cancelled.incrementAndGet();
        check();
    }

    private void done() {
        if (!errors.isEmpty()) {
            target.fail(ThrowableUtils.buildCollectedException(errors));
            return;
        }

        if (cancelled.get() > 0) {
            target.cancel();
            return;
        }

        target.resolve(null);
    }

    private void check() throws Exception {
        if (countdown.decrementAndGet() == 0)
            done();
    }
}