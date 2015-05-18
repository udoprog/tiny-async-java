package eu.toolchain.async.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.Collector;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.TinyThrowableUtils;

/**
 * Helper class for {@link TinyAsync#collect(Collection, Collector)}
 * 
 * @param <S> the source type being collected.
 * @param <T> the target type, the collected sources are being transformed into.
 */
public class CollectHelper<S, T> implements FutureDone<S> {
    private final Collector<S, T> collector;
    private final ResolvableFuture<? super T> target;

    private final int size;
    private final Entry[] results;
    private final AtomicInteger countdown;

    /* maintain position separate since the is a potential race condition between getting the current position and
     * setting the entry. This is avoided by only relying on countdown to trigger when we are done. */
    private final AtomicInteger position = new AtomicInteger();

    public CollectHelper(int size, Collector<S, T> collector, ResolvableFuture<? super T> target) {
        if (size <= 0)
            throw new IllegalArgumentException("size");

        this.size = size;
        this.collector = collector;
        this.target = target;
        this.results = entryArray(size);
        this.countdown = new AtomicInteger(size);
    }

    private Entry[] entryArray(int size) {
        final Entry[] entries = new Entry[size];

        for (int i = 0; i < size; i++)
            entries[i] = new Entry();

        return entries;
    }

    @Override
    public void failed(Throwable e) throws Exception {
        add(position.getAndIncrement(), Entry.ERROR, e);
    }

    @Override
    public void resolved(S result) throws Exception {
        add(position.getAndIncrement(), Entry.RESULT, result);
    }

    @Override
    public void cancelled() throws Exception {
        add(position.getAndIncrement(), Entry.CANCEL, null);
    }

    /**
     * Checks in a call back. It also wraps up the group if all the callbacks have checked in.
     */
    private void add(final int p, final byte type, final Object value) {
        // could technically wrap around, but that would be a minor issue.
        final int c = countdown.decrementAndGet();

        if (p < size) {
            final Entry e = results[p];
            e.type = type;
            e.value = value;
        }

        if (c == 0) {
            final Results<S> r = readResults();
            done(r.results, r.errors, r.cancelled);
        }
    }

    private void done(Collection<S> results, Collection<Throwable> errors, int cancelled) {
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
    private Results<S> readResults() {
        final List<S> results = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        int cancelled = 0;

        for (final Entry e : this.results) {
            switch (e.type) {
            case Entry.ERROR:
                errors.add((Throwable) e.value);
                break;
            case Entry.RESULT:
                results.add((S) e.value);
                break;
            case Entry.CANCEL:
                cancelled++;
                break;
            default:
                throw new IllegalArgumentException("Invalid entry type: " + e.type);
            }
        }

        return new Results<S>(results, errors, cancelled);
    }

    private static final class Entry {
        private static final byte RESULT = 0x01;
        private static final byte ERROR = 0x02;
        private static final byte CANCEL = 0x03;

        private byte type;
        private Object value;
    }

    @RequiredArgsConstructor
    private static class Results<T> {
        private final List<T> results;
        private final List<Throwable> errors;
        private final int cancelled;
    }
}