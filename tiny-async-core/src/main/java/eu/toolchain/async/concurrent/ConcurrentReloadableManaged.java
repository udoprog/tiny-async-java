package eu.toolchain.async.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Borrowed;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.Managed;
import eu.toolchain.async.ManagedAction;
import eu.toolchain.async.ManagedSetup;
import eu.toolchain.async.ReloadableManaged;
import eu.toolchain.async.concurrent.ConcurrentManaged.ManagedState;

public class ConcurrentReloadableManaged<T> implements ReloadableManaged<T> {
    private final AsyncFramework async;
    private final ManagedSetup<T> setup;

    private final AtomicReference<Managed<T>> current;

    protected final AtomicReference<ManagedState> state = new AtomicReference<ManagedState>(ManagedState.INITIALIZED);

    public static <C> ReloadableManaged<C> newReloadableManaged(final AsyncFramework async, final ManagedSetup<C> setup) {
        return new ConcurrentReloadableManaged<C>(async, setup);
    }

    protected ConcurrentReloadableManaged(final AsyncFramework async, final ManagedSetup<T> setup) {
        this.async = async;
        this.setup = setup;

        this.current = new AtomicReference<Managed<T>>(ConcurrentManaged.newManaged(async, setup));
    }

    @Override
    public <R> AsyncFuture<R> doto(final ManagedAction<T, R> action) {
        final Borrowed<T> b = borrow();

        if (!b.isValid()) {
            return async.cancelled();
        }

        final T reference = b.get();

        final AsyncFuture<R> f;

        try {
            f = action.action(reference);
        } catch (Exception e) {
            b.release();
            return async.failed(e);
        }

        return f.onFinished(b.releasing());
    }

    @Override
    public Borrowed<T> borrow() {
        Managed<T> prev = null;

        while (true) {
            final Managed<T> delegate = current.get();

            // if delegate has not changed since last check, borrowing again is useless.
            if ((delegate == null) || (delegate == prev)) {
                return ConcurrentManaged.invalid();
            }

            final Borrowed<T> b = delegate.borrow();

            // re-check the results, we might have caught a reference which was being stopped.
            if (!b.isValid()) {
                prev = delegate;
                continue;
            }

            return b;
        }
    }

    @Override
    public boolean isReady() {
        final Managed<T> delegate = current.get();

        if (delegate == null) {
            return false;
        }

        return delegate.isReady();
    }

    @Override
    public AsyncFuture<Void> start() {
        final Managed<T> delegate = current.get();

        if (delegate == null) {
            return async.cancelled();
        }

        return delegate.start();
    }

    @Override
    public AsyncFuture<Void> stop() {
        final Managed<T> delegate = current.getAndSet(null);

        if (delegate == null) {
            return async.cancelled();
        }

        return delegate.stop();
    }

    @Override
    public AsyncFuture<Void> reload(boolean startFirst) {
        final Managed<T> c = current.get();

        // old is already stopping...
        if (c == null) {
            return async.cancelled();
        }

        final Borrowed<T> b = c.borrow();

        if (!b.isValid()) {
            return async.cancelled();
        }

        final Managed<T> next = ConcurrentManaged.newManaged(async, setup);

        if (startFirst) {
            return startThenStop(b, next);
        }

        return stopThenStart(b, next);
    }

    protected AsyncFuture<Void> stopThenStart(final Borrowed<T> b, final Managed<T> next) {
        while (true) {
            final Managed<T> old = current.get();

            // we are stopping
            if (old == null) {
                b.release();
                return async.cancelled();
            }

            if (!current.compareAndSet(old, next)) {
                continue;
            }

            // swap successful, now we can now safely release old.
            b.release();

            // stop old.
            return old.stop().lazyTransform(new LazyTransform<Void, Void>() {
                @Override
                public AsyncFuture<Void> transform(Void result) throws Exception {
                    return next.start();
                }
            });
        }
    }

    protected AsyncFuture<Void> startThenStop(final Borrowed<T> b, final Managed<T> next) {
        return next.start().lazyTransform(new LazyTransform<Void, Void>() {
            @Override
            public AsyncFuture<Void> transform(Void result) throws Exception {
                while (true) {
                    final Managed<T> old = current.get();

                    // we are stopping
                    // block old from successfully stopping until this one has been cleaned up.
                    if (old == null) {
                        return next.stop().onFinished(b.releasing());
                    }

                    if (!current.compareAndSet(old, next)) {
                        continue;
                    }

                    // swap successful, now we can now safely release old.
                    b.release();

                    // stopping old.
                    return old.stop();
                }
            }
        });
    }

    @Override
    public String toString() {
        final Managed<T> delegate = current.get();

        if (delegate == null) {
            return "<no managed>";
        }

        return delegate.toString();
    }
}