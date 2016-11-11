package eu.toolchain.concurrent;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractImmediateCompletionStage<T> implements CompletionStage<T> {
  protected FutureFramework async;

  public AbstractImmediateCompletionStage(FutureFramework async) {
    this.async = async;
  }

  protected <U> CompletionStage<U> transformResolved(
      final Function<? super T, ? extends U> transform, final T result
  ) {
    final U transformed;

    try {
      transformed = transform.apply(result);
    } catch (final Exception e) {
      return async.failed(e);
    }

    return async.completed(transformed);
  }

  protected <U> CompletionStage<U> lazyTransformResolved(
      final Function<? super T, ? extends CompletionStage<U>> transform, final T result
  ) {
    try {
      return transform.apply(result);
    } catch (final Exception e) {
      return async.failed(e);
    }
  }

  protected <U> CompletionStage<U> transformFailed(
      final Function<? super Throwable, ? extends U> transform, final Throwable cause
  ) {
    final U result;

    try {
      result = transform.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return async.failed(e);
    }

    return async.completed(result);
  }

  protected CompletionStage<T> lazyTransformFailed(
      final Function<? super Throwable, ? extends CompletionStage<T>> transform,
      final Throwable cause
  ) {
    try {
      return transform.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return async.failed(e);
    }
  }

  protected CompletionStage<T> transformCancelled(final Supplier<? extends T> transform) {
    final T transformed;

    try {
      transformed = transform.get();
    } catch (final Exception e) {
      return async.failed(e);
    }

    return async.completed(transformed);
  }

  protected CompletionStage<T> lazyTransformCancelled(
      final Supplier<? extends CompletionStage<T>> transform
  ) {
    try {
      return transform.get();
    } catch (final Exception e) {
      return async.failed(e);
    }
  }
}
