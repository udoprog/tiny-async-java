package eu.toolchain.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractImmediate<T> implements Stage<T> {
  protected Caller caller;

  public AbstractImmediate(final Caller caller) {
    this.caller = caller;
  }

  <U> Stage<U> immediateApply(
      final Function<? super T, ? extends U> fn, final T result
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(result));
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  <U> Stage<U> immediateCompose(
      final Function<? super T, ? extends Stage<U>> fn, final T result
  ) {
    try {
      return fn.apply(result);
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> immediateCatchFailed(
      final Function<? super Throwable, ? extends T> fn, final Throwable cause
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(cause));
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> immediateComposeFailed(
      final Function<? super Throwable, ? extends Stage<T>> fn, final Throwable cause
  ) {
    try {
      return fn.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> immediateCatchCancelled(final Supplier<? extends T> fn) {
    try {
      return new ImmediateCompleted<>(caller, fn.get());
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> immediateComposeCancelled(
      final Supplier<? extends Stage<T>> fn
  ) {
    try {
      return fn.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> immediateWithCloserCancelled(final Supplier<? extends Stage<Void>> notComplete) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenCancel();
  }

  Stage<T> immediateWithCloserFailed(
      final Throwable throwable, final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      final ExecutionException ee = new ExecutionException(e);
      ee.addSuppressed(throwable);
      return new ImmediateFailed<>(caller, ee);
    }

    return next.thenFail(throwable);
  }

  Stage<T> immediateWithCloserCompleted(
      final T result, final Supplier<? extends Stage<Void>> complete,
      final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = complete.get();
    } catch (final Exception e) {
      return notComplete.get().thenFail(e);
    }

    return next.thenComplete(result).withNotComplete(notComplete);
  }

  Stage<T> immediateWithCompleteCompleted(
      final T result, final Supplier<? extends Stage<Void>> complete
  ) {
    final Stage<Void> next;

    try {
      next = complete.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenApply(v -> result);
  }

  Stage<T> immediateWithNotCompleteFailed(
      final Throwable throwable, final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      final ExecutionException ee = new ExecutionException(e);
      ee.addSuppressed(throwable);
      return new ImmediateFailed<>(caller, ee);
    }

    return next.thenFail(throwable);
  }

  Stage<T> immediateWithNotCompleteCancelled(final Supplier<? extends Stage<Void>> supplier) {
    final Stage<Void> next;

    try {
      next = supplier.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenCancel();
  }
}
