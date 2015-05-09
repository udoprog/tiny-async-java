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

public class TinyManaged<T> implements Managed<T> {
    private static final boolean TRACING;
    private static final boolean CAPTURE_STACK;

    static {
        TRACING = "yes".equals(System.getProperty(Managed.TRACING, "no"));
        CAPTURE_STACK = "yes".equals(System.getProperty(Managed.CAPTURE_STACK, "no"));
    }

    private static final StackTraceElement[] EMPTY_STACK = new StackTraceElement[0];

    private final AsyncFramework async;
    private final ManagedSetup<T> setup;

    // the managed reference.
    private final AtomicReference<T> reference = new AtomicReference<>();

    // acts to allow only a single thread to setup the reference.
    private volatile ResolvableFuture<Void> startFuture = null;

    private volatile ResolvableFuture<Void> zeroLeaseFuture = null;

    private volatile AsyncFuture<Void> stopFuture = null;

    private final Set<ValidBorrowed> traces;

    public TinyManaged(final AsyncFramework async, final ManagedSetup<T> setup) {
        this.async = async;
        this.setup = setup;

        if (TRACING) {
            traces = Collections.newSetFromMap(new ConcurrentHashMap<ValidBorrowed, Boolean>());
        } else {
            traces = null;
        }
    }

    private final Object $lock = new Object();

    /**
     * The number of borrowed references that are out there.
     */
    private final AtomicInteger leases = new AtomicInteger(1);

    @Override
    public <R> AsyncFuture<R> doto(final ManagedAction<T, R> action) {
        // pre-emptively increase the number of leases in order to prevent the underlying object (if valid) to be
        // allocated.

        final Borrowed<T> b = borrow();

        final T reference = b.get();

        if (reference == null)
            return async.cancelled();

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
    @Override
    public Borrowed<T> borrow() {
        // pre-emptively increase the number of leases in order to prevent the underlying object (if valid) to be
        // allocated.
        leases.incrementAndGet();

        final T value = reference.get();
        final StackTraceElement[] stack = getStackTrace();

        if (value == null) {
            release(value, stack);
            return new InvalidBorrowed<>();
        }

        final ValidBorrowed b = new ValidBorrowed(value, stack);

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
        synchronized ($lock) {
            if (startFuture != null)
                return startFuture;

            startFuture = async.future();
        }

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
        synchronized ($lock) {
            if (startFuture == null)
                throw new IllegalStateException("not started");

            if (zeroLeaseFuture == null) {
                zeroLeaseFuture = async.future();

                // stop future depends on successful start, then on zero leases.
                stopFuture = startFuture.transform(new LazyTransform<Void, Void>() {
                    @Override
                    public AsyncFuture<Void> transform(Void result) throws Exception {
                        return zeroLeaseFuture;
                    }
                });
            }
        }

        final T value = reference.getAndSet(null);

        if (value == null)
            return stopFuture;

        final StackTraceElement[] stack = getStackTrace();

        // release self-reference.
        release(value, stack);

        return stopFuture.transform(new LazyTransform<Void, Void>() {
            @Override
            public AsyncFuture<Void> transform(Void result) throws Exception {
                return setup.destruct(value);
            }
        });
    }

    private void release(T reference, StackTraceElement[] stack) {
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
            return null;
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

            TinyManaged.this.release(reference, stack);
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
            return String.format("Managed(%s)", reference);

        return formatTraces(traces, reference);
    }

    private String formatTraces(final List<ValidBorrowed> traces, final T reference) {
        final StringBuilder builder = new StringBuilder();

        builder.append(String.format("Managed(%s:\n", reference));

        int i = 0;

        for (final ValidBorrowed b : traces) {
            builder.append(String.format("#%d\n", i++));
            builder.append(formatStack(b.stack) + "\n");
        }

        builder.append(")");
        return builder.toString();
    }

    private static String formatStack(StackTraceElement[] stack) {
        if (stack == null)
            return "unknown";

        final StringBuilder builder = new StringBuilder();

        for (final StackTraceElement e : stack) {
            builder.append(String.format("    %s#%s (%s:%d)\n", e.getClassName(), e.getMethodName(), e.getFileName(),
                    e.getLineNumber()));
        }

        return builder.toString();
    }

    private StackTraceElement[] getStackTrace() {
        if (!CAPTURE_STACK)
            return EMPTY_STACK;

        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stack, 0, stack.length - 2);
    }
}
