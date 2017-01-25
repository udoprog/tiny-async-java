package eu.toolchain.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for immediate computations.
 *
 * <p>This class is intended to be extended by {@link Stage} implementations to provide a basis for
 * immediate implementations for some of the common behaviours of a stage.
 *
 * <p>A prominent reason to make use of this class is that all user-provided code is guarded with a
 * try-catch and implements fallbacks for when exceptions have been thrown in the provided
 * operation.
 *
 * @param <T> result type of the stage
 */
@RequiredArgsConstructor
abstract class AbstractImmediate<T> implements Stage<T> {
  protected final Caller caller;

  <U> Stage<U> thenApplyCompleted(
      final Function<? super T, ? extends U> fn, final T value
  ) {
    final U result;

    try {
      result = fn.apply(value);
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return new ImmediateCompleted<>(caller, result);
  }

  <U> Stage<U> thenComposeCompleted(
      final Function<? super T, ? extends Stage<U>> fn, final T value
  ) {
    try {
      return fn.apply(value);
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  Stage<T> thenApplyCaughtFailed(
      final Function<? super Throwable, ? extends T> fn, final Throwable cause
  ) {
    final T value;

    try {
      value = fn.apply(cause);
    } catch (final Exception e) {
      return executionExceptionFailed(e, cause);
    }

    return new ImmediateCompleted<>(caller, value);
  }

  Stage<T> thenComposeFailedFailed(
      final Function<? super Throwable, ? extends Stage<T>> fn, final Throwable cause
  ) {
    try {
      return fn.apply(cause);
    } catch (final Exception e) {
      return executionExceptionFailed(e, cause);
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
      final Throwable cause, final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      return executionExceptionFailed(e, cause);
    }

    return next.thenFail(cause);
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
      final Throwable cause, final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      return executionExceptionFailed(e, cause);
    }

    return next.thenFail(cause);
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

  Stage<T> executionExceptionFailed(final Throwable e, final Throwable cause) {
    final ExecutionException ee = new ExecutionException(e);
    ee.addSuppressed(cause);
    return new ImmediateFailed<>(caller, ee);
  }
}
