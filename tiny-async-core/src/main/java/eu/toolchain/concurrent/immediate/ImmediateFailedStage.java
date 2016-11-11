package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediateCompletionStage;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.FutureFramework;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A callback which has already been resolved as 'failed'.
 *
 * @param <T>
 */
public class ImmediateFailedStage<T> extends AbstractImmediateCompletionStage<T>
    implements CompletionStage<T> {
  private final FutureCaller caller;
  private final Throwable cause;

  public ImmediateFailedStage(
      FutureFramework async, FutureCaller caller, Throwable cause
  ) {
    super(async);
    this.caller = caller;
    this.cause = cause;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public CompletionStage<T> bind(CompletionStage<?> other) {
    return this;
  }

  @Override
  public CompletionStage<T> handle(CompletionHandle<? super T> handle) {
    caller.fail(handle, cause);
    return this;
  }

  @Override
  public CompletionStage<T> whenFinished(Runnable runnable) {
    caller.finish(runnable);
    return this;
  }

  @Override
  public CompletionStage<T> whenCancelled(Runnable runnable) {
    return this;
  }

  @Override
  public CompletionStage<T> whenCompleted(Consumer<? super T> consumer) {
    return this;
  }

  @Override
  public CompletionStage<T> whenFailed(Consumer<? super Throwable> consumer) {
    caller.fail(consumer, cause);
    return this;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isCompleted() {
    return false;
  }

  @Override
  public boolean isFailed() {
    return true;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public Throwable cause() {
    return cause;
  }

  @Override
  public T join() throws ExecutionException {
    throw new ExecutionException(cause);
  }

  @Override
  public T join(long timeout, TimeUnit unit) throws ExecutionException {
    throw new ExecutionException(cause);
  }

  @Override
  public T joinNow() throws ExecutionException {
    throw new ExecutionException(cause);
  }

  @Override
  public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
    return async.failed(cause);
  }

  @Override
  public <U> CompletionStage<U> thenCompose(
      Function<? super T, ? extends CompletionStage<U>> fn
  ) {
    return async.failed(cause);
  }

  @Override
  public CompletionStage<T> thenCatchFailed(Function<? super Throwable, ? extends T> fn) {
    return transformFailed(fn, cause);
  }

  @Override
  public CompletionStage<T> thenComposeFailed(
      Function<? super Throwable, ? extends CompletionStage<T>> fn
  ) {
    return lazyTransformFailed(fn, cause);
  }

  @Override
  public CompletionStage<T> thenCatchCancelled(Supplier<? extends T> supplier) {
    return this;
  }

  @Override
  public CompletionStage<T> thenComposeCancelled(Supplier<? extends CompletionStage<T>> supplier) {
    return this;
  }
}
