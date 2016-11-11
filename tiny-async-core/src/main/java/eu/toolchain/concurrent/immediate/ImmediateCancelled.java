package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediate;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A callback which has already been resolved as 'resolved'.
 *
 * @param <T>
 */
@EqualsAndHashCode(of = {}, doNotUseGetters = true, callSuper = false)
@ToString(of = {})
public class ImmediateCancelled<T> extends AbstractImmediate<T> implements CompletionStage<T> {
  private final FutureCaller caller;

  public ImmediateCancelled(final FutureCaller caller) {
    super(caller);
    this.caller = caller;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public CompletionStage<T> handle(CompletionHandle<? super T> handle) {
    caller.cancel(handle);
    return this;
  }

  @Override
  public CompletionStage<T> bind(CompletionStage<?> other) {
    other.cancel();
    return this;
  }

  @Override
  public CompletionStage<T> whenFinished(Runnable runnable) {
    caller.finish(runnable);
    return this;
  }

  @Override
  public CompletionStage<T> whenCancelled(Runnable runnable) {
    caller.cancel(runnable);
    return this;
  }

  @Override
  public CompletionStage<T> whenCompleted(Consumer<? super T> consumer) {
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
    return false;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return true;
  }

  @Override
  public Throwable cause() {
    throw new IllegalStateException("future is not in a failed state");
  }

  @Override
  public T join() {
    throw new CancellationException();
  }

  @Override
  public T join(long timeout, TimeUnit unit) {
    throw new CancellationException();
  }

  @Override
  public T joinNow() {
    throw new CancellationException();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
    return new ImmediateCancelled<>(caller);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> CompletionStage<U> thenCompose(
      Function<? super T, ? extends CompletionStage<U>> fn
  ) {
    return new ImmediateCancelled<>(caller);
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
    return immediateCatchCancelled(supplier);
  }

  @Override
  public CompletionStage<T> thenComposeCancelled(Supplier<? extends CompletionStage<T>> supplier) {
    return immediateComposeCancelled(supplier);
  }
}
