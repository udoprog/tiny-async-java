package eu.toolchain.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

class ConcurrentReloadableManaged<T> implements ReloadableManaged<T> {
  private final Async async;
  private final Caller caller;
  private final ManagedOptions managedOptions;
  private final Supplier<? extends Stage<T>> setup;
  private final Function<? super T, ? extends Stage<Void>> teardown;

  private final AtomicReference<ConcurrentManaged<T>> current;

  ConcurrentReloadableManaged(
      final Async async, final Caller caller, final ManagedOptions managedOptions,
      final Supplier<? extends Stage<T>> setup,
      final Function<? super T, ? extends Stage<Void>> teardown
  ) {
    this.async = async;
    this.caller = caller;
    this.setup = setup;
    this.teardown = teardown;
    this.managedOptions = managedOptions;

    this.current = new AtomicReference<>(newManaged());
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
    } catch (final Exception e) {
      b.release();
      return async.failed(e);
    }

    return f.whenDone(b::release);
  }

  @Override
  public Borrowed<T> borrow() {
    Managed<T> previous = null;

    while (true) {
      final Managed<T> current = this.current.get();

      // we are stopping, nothing to borrow
      if (current == null) {
        return ConcurrentManaged.invalid();
      }

      // nothing has change since last iteration, borrowing again is useless.
      if (current == previous) {
        return ConcurrentManaged.invalid();
      }

      final Borrowed<T> b = current.borrow();

      // re-check the results, we might have caught a reference which was being stopped.
      if (b.isValid()) {
        return b;
      }

      previous = current;
    }
  }

  @Override
  public boolean isReady() {
    final Managed<T> delegate = current.get();
    return delegate != null && delegate.isReady();
  }

  @Override
  public Stage<Void> start() {
    final Managed<T> delegate = current.get();

    // stopped
    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.start();
  }

  @Override
  public Stage<Void> stop() {
    final Managed<T> delegate = current.getAndSet(null);

    // already stopped
    if (delegate == null) {
      return async.cancelled();
    }

    return delegate.stop();
  }

  @Override
  public Stage<Void> reload() {
    final ConcurrentManaged<T> previous = current.get();

    // we are stopping
    if (previous == null) {
      return async.cancelled();
    }

    final ConcurrentManaged<T> next = newManaged();

    return next.start().thenCompose(result -> {
      while (true) {
        final ConcurrentManaged<T> old = current.get();

        // we are stopping
        // block old from successfully stopping until this one has been cleaned up.
        if (old == null) {
          return next.stop();
        }

        if (!current.compareAndSet(old, next)) {
          continue;
        }

        // stopping old
        return old.stop();
      }
    });
  }

  /**
   * Construct a new managed reference.
   *
   * @return a new concurrent managed
   */
  private ConcurrentManaged<T> newManaged() {
    return ConcurrentManaged.newManaged(async, caller, managedOptions, setup, teardown);
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
