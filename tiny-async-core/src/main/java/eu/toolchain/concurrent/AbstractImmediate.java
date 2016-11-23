package eu.toolchain.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractImmediate<T> implements Stage<T> {
  protected Caller caller;

  public AbstractImmediate(final Caller caller) {
    this.caller = caller;
  }

  <U> Stage<U> thenApplyCompleted(
      final Function<? super T, ? extends U> fn, final T result
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(result));
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  <U> Stage<U> thenComposeCompleted(
      final Function<? super T, ? extends Stage<U>> fn, final T result
  ) {
    try {
      return fn.apply(result);
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> thenApplyCaughtFailed(
      final Function<? super Throwable, ? extends T> fn, final Throwable cause
  ) {
    try {
      return new ImmediateCompleted<>(caller, fn.apply(cause));
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> thenComposeFailedFailed(
      final Function<? super Throwable, ? extends Stage<T>> fn, final Throwable cause
  ) {
    try {
      return fn.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> thenSupplyCancelledCancelled(final Supplier<? extends T> fn) {
    try {
      return new ImmediateCompleted<>(caller, fn.get());
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> thenComposeCancelledCancelled(
      final Supplier<? extends Stage<T>> fn
  ) {
    try {
      return fn.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> withCloserCancelled(final Supplier<? extends Stage<Void>> notComplete) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenCancel();
  }

  Stage<T> withCloserFailed(
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

  Stage<T> withCloserCompleted(
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

  Stage<T> withCompleteCompleted(
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

  Stage<T> withNotCompleteFailed(
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

  Stage<T> withNotCompleteCancelled(final Supplier<? extends Stage<Void>> supplier) {
    final Stage<Void> next;

    try {
      next = supplier.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenCancel();
  }
}
