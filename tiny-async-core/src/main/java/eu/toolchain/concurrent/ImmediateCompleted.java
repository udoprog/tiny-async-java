package eu.toolchain.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A future which has already been completed.
 *
 * @param <T> type of the future
 */
@EqualsAndHashCode(of = {"result"}, doNotUseGetters = true, callSuper = false)
@ToString(of = {"result"})
public class ImmediateCompleted<T> extends AbstractImmediate<T>
    implements CompletionStage<T> {
  private final FutureCaller caller;
  private final T result;

  public ImmediateCompleted(
      final FutureCaller caller, final T result
  ) {
    super(caller);
    this.caller = caller;
    this.result = result;
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
    caller.complete(handle, result);
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
    caller.complete(consumer, result);
    return this;
  }

  @Override
  public CompletionStage<T> whenFailed(Consumer<? super Throwable> consumer) {
    return this;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isCompleted() {
    return true;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public Throwable cause() {
    throw new IllegalStateException("future is not in a failed state");
  }

  @Override
  public T join() {
    return result;
  }

  @Override
  public T join(long timeout, TimeUnit unit) {
    return result;
  }

  @Override
  public T joinNow() {
    return result;
  }

  @Override
  public <R> CompletionStage<R> thenApply(Function<? super T, ? extends R> fn) {
    return immediateApply(fn, result);
  }

  @Override
  public <R> CompletionStage<R> thenCompose(
      Function<? super T, ? extends CompletionStage<R>> fn
  ) {
    return immediateCompose(fn, result);
  }

  @Override
  public CompletionStage<T> thenCatchFailed(Function<? super Throwable, ? extends T> fn) {
    return this;
  }

  @Override
  public CompletionStage<T> thenComposeFailed(
      Function<? super Throwable, ? extends CompletionStage<T>> fn
  ) {
    return this;
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
