package eu.toolchain.async.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Collector;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.ThrowableUtils;

/**
 * Implementation of {@link AsyncFuture#end(List, Collector)}.
 *
 * @author udoprog
 *
 * @param <T>
 */
public class FutureCollector<S, T> implements FutureDone<S> {
    private final int size;
    private final Collector<S, T> reducable;
    private final ResolvableFuture<T> target;

    private final AtomicReferenceArray<Entry> results;
    private final AtomicInteger position = new AtomicInteger();

    public FutureCollector(int size, Collector<S, T> reducable, ResolvableFuture<T> target) {
        this.size = size;
        this.reducable = reducable;
        this.target = target;
        this.results = new AtomicReferenceArray<>(size);
    }

    @Override
    public void failed(Throwable e) throws Exception {
        add(position.getAndIncrement(), new Entry(Entry.ERROR, e));
    }

    @Override
    public void resolved(S result) throws Exception {
        add(position.getAndIncrement(), new Entry(Entry.RESULT, result));
    }

    @Override
    public void cancelled() throws Exception {
        add(position.getAndIncrement(), new Entry(Entry.CANCEL, null));
    }

    /**
     * Checks in a call back. It also wraps up the group if all the callbacks have checked in.
     */
    private void add(final int p, final Entry entry) {
        if (p >= size)
            throw new IllegalStateException("too many results received, expected " + size + " but got " + p);

        results.set(p, entry);

        // waiting for more results.
        if (p + 1 < size)
            return;

        final Results<S> r = readResults();

        done(r.results, r.errors, r.cancelled);
    }

    private void done(Collection<S> results, Collection<Throwable> errors, int cancelled) {
        if (!errors.isEmpty()) {
            target.fail(ThrowableUtils.buildCollectedException(errors));
            return;
        }

        if (cancelled > 0) {
            target.cancel();
            return;
        }

        T result;

        try {
            result = reducable.collect(results);
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

        for (int i = 0; i < size; i++) {
            final Entry e = this.results.get(i);

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

    @RequiredArgsConstructor
    private static final class Entry {
        private static final byte RESULT = 0x00;
        private static final byte ERROR = 0x01;
        private static final byte CANCEL = 0x02;

        private final byte type;
        private final Object value;
    }

    @RequiredArgsConstructor
    private static class Results<T> {
        private final List<T> results;
        private final List<Throwable> errors;
        private final int cancelled;
    }
}