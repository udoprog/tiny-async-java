package eu.toolchain.concurrent;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractImmediate<T> implements Stage<T> {
  protected FutureCaller caller;

  public AbstractImmediate(final FutureCaller caller) {
    this.caller = caller;
  }

  protected <U> Stage<U> immediateApply(
      final Function<? super T, ? extends U> fn, final T result
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(result));
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  protected <U> Stage<U> immediateCompose(
      final Function<? super T, ? extends Stage<U>> fn, final T result
  ) {
    try {
      return fn.apply(result);
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  protected Stage<T> immediateCatchFailed(
      final Function<? super Throwable, ? extends T> fn, final Throwable cause
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(cause));
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  protected Stage<T> immediateComposeFailed(
      final Function<? super Throwable, ? extends Stage<T>> fn, final Throwable cause
  ) {
    try {
      return fn.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  protected Stage<T> immediateCatchCancelled(final Supplier<? extends T> fn) {
    try {
      return new ImmediateCompleted<>(caller, fn.get());
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  protected Stage<T> immediateComposeCancelled(
      final Supplier<? extends Stage<T>> fn
  ) {
    try {
      return fn.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }
}
