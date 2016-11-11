package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediate;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A callback which has already been resolved as 'failed'.
 *
 * @param <T>
 */
@EqualsAndHashCode(of = {"cause"}, doNotUseGetters = true, callSuper = false)
@ToString(of = {"cause"})
public class ImmediateFailed<T> extends AbstractImmediate<T>
    implements CompletionStage<T> {
  private final FutureCaller caller;
  private final Throwable cause;

  public ImmediateFailed(
      FutureCaller caller, Throwable cause
  ) {
    super(caller);
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

  @SuppressWarnings("unchecked")
  @Override
  public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
    return new ImmediateFailed<>(caller, cause);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> CompletionStage<U> thenCompose(
      Function<? super T, ? extends CompletionStage<U>> fn
  ) {
    return new ImmediateFailed<>(caller, cause);
  }

  @Override
  public CompletionStage<T> thenCatchFailed(Function<? super Throwable, ? extends T> fn) {
    return immediateCatchFailed(fn, cause);
  }

  @Override
  public CompletionStage<T> thenComposeFailed(
      Function<? super Throwable, ? extends CompletionStage<T>> fn
  ) {
    return immediateComposeFailed(fn, cause);
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
