package eu.toolchain.concurrent.concurrent;

import eu.toolchain.concurrent.Borrowed;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.FutureFramework;
import eu.toolchain.concurrent.Managed;
import eu.toolchain.concurrent.ManagedAction;
import eu.toolchain.concurrent.ReloadableManaged;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrentReloadableManaged<T> implements ReloadableManaged<T> {
  private final FutureFramework async;
  private final FutureCaller caller;
  private final Supplier<? extends CompletionStage<T>> setup;
  private final Function<? super T, ? extends CompletionStage<Void>> teardown;

  private final AtomicReference<Managed<T>> current;

  public static <C> ReloadableManaged<C> newReloadableManaged(
      final FutureFramework async, final FutureCaller caller,
      final Supplier<? extends CompletionStage<C>> setup,
      final Function<? super C, ? extends CompletionStage<Void>> teardown
  ) {
    return new ConcurrentReloadableManaged<>(async, caller, setup, teardown);
  }

  ConcurrentReloadableManaged(
      final FutureFramework async, final FutureCaller caller,
      final Supplier<? extends CompletionStage<T>> setup,
      final Function<? super T, ? extends CompletionStage<Void>> teardown
  ) {
    this.async = async;
    this.caller = caller;
    this.setup = setup;
    this.teardown = teardown;

    this.current =
        new AtomicReference<>(ConcurrentManaged.newManaged(async, caller, setup, teardown));
  }

  @Override
  public <R> CompletionStage<R> doto(final ManagedAction<T, R> action) {
    final Borrowed<T> b = borrow();

    if (!b.isValid()) {
      return async.cancelled();
    }

    final T reference = b.get();

    final CompletionStage<R> f;

    try {
      f = action.action(reference);
    } catch (Exception e) {
      b.release();
      return async.failed(e);
    }

    return f.whenFinished(b.releasing());
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
  public CompletionStage<Void> start() {
    final Managed<T> delegate = current.get();

    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.start();
  }

  @Override
  public CompletionStage<Void> stop() {
    final Managed<T> delegate = current.getAndSet(null);

    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.stop();
  }

  @Override
  public CompletionStage<Void> reload(boolean startFirst) {
    final Managed<T> c = current.get();

    // old is already stopping...
    if (c == null) {
      return async.cancelled();
    }

    final Borrowed<T> b = c.borrow();

    if (!b.isValid()) {
      return async.cancelled();
    }

    final Managed<T> next = ConcurrentManaged.newManaged(async, caller, setup, teardown);

    if (startFirst) {
      return startThenStop(b, next);
    }

    return stopThenStart(b, next);
  }

  protected CompletionStage<Void> stopThenStart(final Borrowed<T> b, final Managed<T> next) {
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
      return old.stop().thenCompose(result -> next.start());
    }
  }

  protected CompletionStage<Void> startThenStop(final Borrowed<T> b, final Managed<T> next) {
    return next.start().thenCompose(result -> {
      while (true) {
        final Managed<T> old = current.get();

        // we are stopping
        // block old from successfully stopping until this one has been cleaned up.
        if (old == null) {
          return next.stop().whenFinished(b.releasing());
        }

        if (!current.compareAndSet(old, next)) {
          continue;
        }

        // swap successful, now we can now safely release old.
        b.release();

        // stopping old.
        return old.stop();
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
