package eu.toolchain.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;

public class ConcurrentManaged<T> implements Managed<T> {
    private static final boolean TRACING;
    private static final boolean CAPTURE_STACK;

    // fetch and compare the value of properties that modifies runtime behaviour of this class.
    static {
        TRACING = "on".equals(System.getProperty(Managed.TRACING, "off"));
        CAPTURE_STACK = "on".equals(System.getProperty(Managed.CAPTURE_STACK, "off"));
    }

    private static final InvalidBorrowed<?> INVALID = new InvalidBorrowed<>();

    private static final StackTraceElement[] EMPTY_STACK = new StackTraceElement[0];

    private final AsyncFramework async;
    private final ManagedSetup<T> setup;

    // the managed reference.
    private final AtomicReference<T> reference = new AtomicReference<>();

    // acts to allow only a single thread to setup the reference.
    private final ResolvableFuture<Void> startFuture;
    private final ResolvableFuture<Void> zeroLeaseFuture;
    private final ResolvableFuture<T> stopReferenceFuture;

    // composite future that depends on zero-lease, and stop-reference.
    private final AsyncFuture<Void> stopFuture;

    private final AtomicReference<ManagedState> state = new AtomicReference<ManagedState>(ManagedState.INITIALIZED);

    private final Set<ValidBorrowed> traces;

    public static <T> ConcurrentManaged<T> newManaged(final AsyncFramework async, final ManagedSetup<T> setup) {
        final ResolvableFuture<Void> startFuture = async.future();
        final ResolvableFuture<Void> zeroLeaseFuture = async.future();
        final ResolvableFuture<T> stopReferenceFuture = async.future();

        final AsyncFuture<Void> stopFuture = zeroLeaseFuture.transform(new LazyTransform<Void, Void>() {
            @Override
            public AsyncFuture<Void> transform(Void v) throws Exception {
                return stopReferenceFuture.transform(new LazyTransform<T, Void>() {
                    @Override
                    public AsyncFuture<Void> transform(T reference) throws Exception {
                        return setup.destruct(reference);
                    }
                });
            }
        });

        return new ConcurrentManaged<T>(async, setup, startFuture, zeroLeaseFuture, stopReferenceFuture, stopFuture);
    }

    protected ConcurrentManaged(final AsyncFramework async, final ManagedSetup<T> setup,
            final ResolvableFuture<Void> startFuture, final ResolvableFuture<Void> zeroLeaseFuture,
            final ResolvableFuture<T> stopReferenceFuture, final AsyncFuture<Void> stopFuture) {
        this.async = async;
        this.setup = setup;

        this.startFuture = startFuture;
        this.zeroLeaseFuture = zeroLeaseFuture;
        this.stopReferenceFuture = stopReferenceFuture;
        this.stopFuture = stopFuture;

        if (TRACING) {
            traces = Collections.newSetFromMap(new ConcurrentHashMap<ValidBorrowed, Boolean>());
        } else {
            traces = null;
        }
    }

    /**
     * The number of borrowed references that are out there.
     */
    private final AtomicInteger leases = new AtomicInteger(1);

    @Override
    public <R> AsyncFuture<R> doto(final ManagedAction<T, R> action) {
        // pre-emptively increase the number of leases in order to prevent the underlying object (if valid) to be
        // allocated.

        final Borrowed<T> b = borrow();

        if (!b.isValid())
            return async.cancelled();

        final T reference = b.get();

        final AsyncFuture<R> f;

        try {
            f = action.action(reference);
        } catch (Exception e) {
            b.release();
            return async.failed(e);
        }

        return f.on(b.releasing());
    }

    /**
     * Get the instance that has only been initialized once.
     *
     * Checks that the reference is 'ready' by calling {@code #isReady()} before trying to fetch reference.
     *
     * @return
     * @throws IllegalStateException If the reference is not ready when calling {@code #get()}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Borrowed<T> borrow() {
        // pre-emptively increase the number of leases in order to prevent the underlying object (if valid) to be
        // allocated.
        leases.incrementAndGet();

        final T value = reference.get();

        if (value == null) {
            release();
            return (Borrowed<T>) INVALID;
        }

        final ValidBorrowed b = new ValidBorrowed(value, getStackTrace());

        if (TRACING)
            traces.add(b);

        return b;
    }

    /**
     * Check if the instance has been initialized or not.
     *
     * @return
     */
    @Override
    public boolean isReady() {
        return startFuture != null && startFuture.isDone();
    }

    @Override
    public AsyncFuture<Void> start() {
        if (!state.compareAndSet(ManagedState.INITIALIZED, ManagedState.STARTED))
            return startFuture;

        return setup.construct().transform(new Transform<T, Void>() {
            @Override
            public Void transform(T result) throws Exception {
                if (result == null)
                    throw new IllegalArgumentException("setup reference must no non-null");

                reference.set(result);
                return null;
            }
        }).on(new FutureDone<Void>() {
            @Override
            public void failed(Throwable cause) throws Exception {
                startFuture.fail(cause);
            }

            @Override
            public void resolved(Void result) throws Exception {
                startFuture.resolve(result);
            }

            @Override
            public void cancelled() throws Exception {
                startFuture.cancel();
            }
        });
    }

    @Override
    public AsyncFuture<Void> stop() {
        if (!state.compareAndSet(ManagedState.STARTED, ManagedState.STOPPED))
            return stopFuture;

        stopReferenceFuture.resolve(this.reference.getAndSet(null));

        // release self-reference.
        release();
        return stopFuture;
    }

    private void release() {
        final int lease = leases.decrementAndGet();

        if (lease == 0)
            zeroLeaseFuture.resolve(null);
    }

    private static class InvalidBorrowed<T> implements Borrowed<T> {
        private static FutureFinished FINISHED = new FutureFinished() {
            @Override
            public void finished() throws Exception {
            }
        };

        @Override
        public void close() {
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("cannot get an invalid borrowed reference");
        }

        @Override
        public void release() {
        }

        @Override
        public FutureFinished releasing() {
            return FINISHED;
        }
    }

    /**
     * Wraps returned references that are taken from this SetupOnce instance.
     */
    @RequiredArgsConstructor
    private class ValidBorrowed implements Borrowed<T> {
        private final T reference;
        private final StackTraceElement[] stack;

        private final AtomicBoolean released = new AtomicBoolean(false);

        @Override
        public T get() {
            return reference;
        }

        @Override
        public void release() {
            if (!released.compareAndSet(false, true))
                return;

            if (TRACING)
                traces.remove(this);

            ConcurrentManaged.this.release();
        }

        @Override
        public FutureFinished releasing() {
            return new FutureFinished() {
                @Override
                public void finished() throws Exception {
                    release();
                }
            };
        }

        @Override
        public void close() {
            release();
        }

        /**
         * Implement to log errors on release errors.
         */
        @Override
        protected void finalize() throws Throwable {
            if (released.get())
                return;

            async.caller().leakedManagedReference(reference, stack);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    @Override
    public String toString() {
        final List<ValidBorrowed> traces = TRACING ? new ArrayList<>(this.traces) : null;
        final T reference = this.reference.get();

        if (traces == null || traces.isEmpty())
            return String.format("Managed(%s, %s)", state, reference);

        return formatTraces(traces, reference);
    }

    private String formatTraces(final List<ValidBorrowed> traces, final T reference) {
        final StringBuilder builder = new StringBuilder();

        builder.append(String.format("Managed(%s, %s:\n", state, reference));

        int i = 0;

        for (final ValidBorrowed b : traces) {
            builder.append(String.format("#%d\n", i++));
            builder.append(TinyStackUtils.formatStack(b.stack) + "\n");
        }

        builder.append(")");
        return builder.toString();
    }

    private StackTraceElement[] getStackTrace() {
        if (!CAPTURE_STACK)
            return EMPTY_STACK;

        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stack, 0, stack.length - 2);
    }

    private static enum ManagedState {
        INITIALIZED, STARTED, STOPPED
    }
}
