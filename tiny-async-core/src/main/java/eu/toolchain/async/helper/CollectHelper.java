package eu.toolchain.async.helper;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Collector;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.TinyThrowableUtils;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for {@link TinyAsync#collect(Collection, Collector)}
 * <p>
 * The helper implements {@code FutureDone}, and is intended to be used by binding it as a listener
 * to the futures being collected.
 * <p>
 * This is a lock-free implementation capable of writing the results out of order.
 *
 * @param <S> the source type being collected.
 * @param <T> the target type, the collected sources are being transformed into.
 */
public class CollectHelper<S, T> implements FutureDone<S> {
    public static final byte RESOLVED = 0x1;
    public static final byte FAILED = 0x2;
    public static final byte CANCELLED = 0x3;

    final Collector<S, T> collector;
    Collection<? extends AsyncFuture<?>> sources;
    final ResolvableFuture<? super T> target;

    final int size;

    /* The collected results, non-final to allow for setting to null. Allows for random writes
    since its a pre-emptively
     * sized array. */ Object[] values;
    byte[] states;

    /* maintain position separate since the is a potential race condition between getting the
    current position and
     * setting the entry. This is avoided by only relying on countdown to trigger when we are
     * done. */
    final AtomicInteger write = new AtomicInteger();

    /* maintain a separate countdown since the write position might be out of order, this causes
    all threads to
     * synchronize after the write */
    final AtomicInteger countdown;

    /* Indicate that collector is finished to avoid the case where the write position wraps
    around. */
    final AtomicBoolean finished = new AtomicBoolean();

    /* On a single failure, cause all other sources to be cancelled */
    final AtomicBoolean failed = new AtomicBoolean();

    public CollectHelper(
        int size, Collector<S, T> collector, Collection<? extends AsyncFuture<?>> sources,
        ResolvableFuture<? super T> target
    ) {
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }

        this.size = size;
        this.collector = collector;
        this.sources = sources;
        this.target = target;
        this.values = new Object[size];
        this.states = new byte[size];
        this.countdown = new AtomicInteger(size);
    }

    @Override
    public void resolved(S result) throws Exception {
        add(RESOLVED, result);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        add(FAILED, e);
        checkFailed();
    }

    @Override
    public void cancelled() throws Exception {
        add(CANCELLED, null);
        checkFailed();
    }

    void checkFailed() {
        if (!failed.compareAndSet(false, true)) {
            return;
        }

        for (final AsyncFuture<?> source : sources) {
            source.cancel();
        }

        // help garbage collection.
        sources = null;
    }

    /**
     * Checks in a call back. It also wraps up the group if all the callbacks have checked in.
     */
    void add(final byte type, final Object value) {
        if (finished.get()) {
            throw new IllegalStateException("already finished");
        }

        final int w = write.getAndIncrement();

        if (w < size) {
            writeAt(w, type, value);
        }

        // countdown could wrap around, however we check the state of finished in here.
        // MUST be called after write to make sure that results and states are synchronized.
        final int c = countdown.decrementAndGet();

        if (c < 0) {
            throw new IllegalStateException("already finished (countdown)");
        }

        // if this thread is not the last thread to check-in, do nothing..
        if (c != 0) {
            return;
        }

        // make sure this can only happen once.
        // This protects against countdown, and write wrapping around which should very rarely
        // happen.
        if (!finished.compareAndSet(false, true)) {
            throw new IllegalStateException("already finished");
        }

        done(collect());
    }

    void writeAt(final int w, final byte state, final Object value) {
        states[w] = state;
        values[w] = value;
    }

    void done(Results r) {
        final Collection<S> results = r.results;
        final Collection<Throwable> errors = r.errors;
        final int cancelled = r.cancelled;

        if (!errors.isEmpty()) {
            target.fail(TinyThrowableUtils.buildCollectedException(errors));
            return;
        }

        if (cancelled > 0) {
            target.cancel();
            return;
        }

        T result;

        try {
            result = collector.collect(results);
        } catch (final Exception error) {
            target.fail(error);
            return;
        }

        target.resolve(result);
    }

    @SuppressWarnings("unchecked")
    Results collect() {
        final List<S> results = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        int cancelled = 0;

        for (int i = 0; i < size; i++) {
            final byte type = states[i];

            switch (type) {
                case RESOLVED:
                    results.add((S) values[i]);
                    break;
                case FAILED:
                    errors.add((Throwable) values[i]);
                    break;
                case CANCELLED:
                    cancelled++;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid entry type: " + type);
            }
        }

        // help garbage collector
        this.states = null;
        this.values = null;

        return new Results(results, errors, cancelled);
    }

    @RequiredArgsConstructor
    class Results {
        private final List<S> results;
        private final List<Throwable> errors;
        private final int cancelled;
    }
}
