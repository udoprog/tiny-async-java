package eu.toolchain.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConcurrentReloadableManaged<T> implements ReloadableManaged<T> {
  private final Async async;
  private final Caller caller;
  private final Supplier<? extends Stage<T>> setup;
  private final Function<? super T, ? extends Stage<Void>> teardown;

  private final AtomicReference<Managed<T>> current;

  public static <C> ReloadableManaged<C> newReloadableManaged(
      final Async async, final Caller caller,
      final Supplier<? extends Stage<C>> setup,
      final Function<? super C, ? extends Stage<Void>> teardown
  ) {
    return new ConcurrentReloadableManaged<>(async, caller, setup, teardown);
  }

  ConcurrentReloadableManaged(
      final Async async, final Caller caller,
      final Supplier<? extends Stage<T>> setup,
      final Function<? super T, ? extends Stage<Void>> teardown
  ) {
    this.async = async;
    this.caller = caller;
    this.setup = setup;
    this.teardown = teardown;

    this.current =
        new AtomicReference<>(ConcurrentManaged.newManaged(async, caller, setup, teardown));
  }

  @Override
  public <U> Stage<U> doto(
      final Function<? super T, ? extends Stage<U>> action
  ) {
    final Borrowed<T> b = borrow();

    if (!b.isValid()) {
      return async.cancelled();
    }

    final T reference = b.get();

    final Stage<U> f;

    try {
      f = action.apply(reference);
    } catch (Exception e) {
      b.release();
      return async.failed(e);
    }

    return f.whenFinished(b::release);
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
  public Stage<Void> start() {
    final Managed<T> delegate = current.get();

    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.start();
  }

  @Override
  public Stage<Void> stop() {
    final Managed<T> delegate = current.getAndSet(null);

    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.stop();
  }

  @Override
  public Stage<Void> reload(boolean startFirst) {
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

  protected Stage<Void> stopThenStart(final Borrowed<T> b, final Managed<T> next) {
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

  protected Stage<Void> startThenStop(final Borrowed<T> b, final Managed<T> next) {
    return next.start().thenCompose(result -> {
      while (true) {
        final Managed<T> old = current.get();

        // we are stopping
        // block old from successfully stopping until this one has been cleaned up.
        if (old == null) {
          return next.stop().whenFinished(b::release);
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
