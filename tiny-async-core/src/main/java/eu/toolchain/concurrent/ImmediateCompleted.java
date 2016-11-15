package eu.toolchain.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A completable which has already been completed.
 *
 * @param <T> type of the completable
 */
@EqualsAndHashCode(of = {"result"}, doNotUseGetters = true, callSuper = false)
@ToString(of = {"result"})
public class ImmediateCompleted<T> extends AbstractImmediate<T> implements Stage<T> {
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
  public Stage<T> whenDone(CompletionHandle<? super T> handle) {
    caller.execute(() -> handle.completed(result));
    return this;
  }

  @Override
  public Stage<T> whenFinished(Runnable runnable) {
    caller.execute(runnable);
    return this;
  }

  @Override
  public Stage<T> whenCancelled(Runnable runnable) {
    return this;
  }

  @Override
  public Stage<T> whenComplete(Consumer<? super T> consumer) {
    caller.execute(() -> consumer.accept(result));
    return this;
  }

  @Override
  public Stage<T> whenFailed(Consumer<? super Throwable> consumer) {
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
    throw new IllegalStateException("completable is not in a failed state");
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
  public <R> Stage<R> thenApply(Function<? super T, ? extends R> fn) {
    return immediateApply(fn, result);
  }

  @Override
  public <R> Stage<R> thenCompose(
      Function<? super T, ? extends Stage<R>> fn
  ) {
    return immediateCompose(fn, result);
  }

  @Override
  public Stage<T> thenApplyFailed(Function<? super Throwable, ? extends T> fn) {
    return this;
  }

  @Override
  public Stage<T> thenComposeFailed(
      Function<? super Throwable, ? extends Stage<T>> fn
  ) {
    return this;
  }

  @Override
  public Stage<T> thenApplyCancelled(Supplier<? extends T> supplier) {
    return this;
  }

  @Override
  public Stage<T> thenComposeCancelled(Supplier<? extends Stage<T>> supplier) {
    return this;
  }
}
