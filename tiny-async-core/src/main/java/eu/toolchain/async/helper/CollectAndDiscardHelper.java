package eu.toolchain.async.helper;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.TinyThrowableUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link AsyncFramework#collectAndDiscard(Collection)}.
 *
 * @param <T> The type being collected and discarded.
 * @author udoprog
 */
public class CollectAndDiscardHelper<T> implements FutureDone<T> {
    private final ResolvableFuture<Void> target;
    private final AtomicInteger countdown;
    private final AtomicInteger cancelled = new AtomicInteger();
    private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

    public CollectAndDiscardHelper(int size, ResolvableFuture<Void> target) {
        this.target = target;
        this.countdown = new AtomicInteger(size);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        errors.add(e);
        check();
    }

    @Override
    public void resolved(T result) throws Exception {
        check();
    }

    @Override
    public void cancelled() throws Exception {
        cancelled.incrementAndGet();
        check();
    }

    private void done() {
        if (!errors.isEmpty()) {
            target.fail(TinyThrowableUtils.buildCollectedException(errors));
            return;
        }

        if (cancelled.get() > 0) {
            target.cancel();
            return;
        }

        target.resolve(null);
    }

    private void check() throws Exception {
        if (countdown.decrementAndGet() == 0) {
            done();
        }
    }
}
