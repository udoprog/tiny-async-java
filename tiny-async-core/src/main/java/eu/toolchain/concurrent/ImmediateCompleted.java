package eu.toolchain.concurrent;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A stage which has already been completed.
 *
 * @param <T> type of the completable
 */
@EqualsAndHashCode(of = {"result"}, doNotUseGetters = true, callSuper = false)
public class ImmediateCompleted<T> extends AbstractImmediate<T> implements Stage<T> {
  private final T result;

  public ImmediateCompleted(
      final Caller caller, final T result
  ) {
    super(caller);
    this.result = result;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public Stage<T> handle(Handle<? super T> handle) {
    caller.execute(() -> handle.completed(result));
    return this;
  }

  @Override
  public Stage<T> whenDone(Runnable runnable) {
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
    return thenApplyCompleted(fn, result);
  }

  @Override
  public <R> Stage<R> thenCompose(
      Function<? super T, ? extends Stage<R>> fn
  ) {
    return thenComposeCompleted(fn, result);
  }

  @Override
  public Stage<T> thenApplyFailed(Function<? super Throwable, ? extends T> fn) {
    return this;
  }

  @Override
  public Stage<T> thenComposeCaught(
      Function<? super Throwable, ? extends Stage<T>> fn
  ) {
    return this;
  }

  @Override
  public Stage<T> withCloser(
      final Supplier<? extends Stage<Void>> complete,
      final Supplier<? extends Stage<Void>> other
  ) {
    return withCloserCompleted(result, complete, other);
  }

  @Override
  public Stage<T> withComplete(
      final Supplier<? extends Stage<Void>> supplier
  ) {
    return withCompleteCompleted(result, supplier);
  }

  @Override
  public Stage<T> withNotComplete(
      final Supplier<? extends Stage<Void>> supplier
  ) {
    return this;
  }

  @Override
  public <U> Stage<U> thenFail(final Throwable cause) {
    return new ImmediateFailed<>(caller, cause);
  }

  @Override
  public <U> Stage<U> thenCancel() {
    return new ImmediateCancelled<>(caller);
  }

  @Override
  public <U> Stage<U> thenComplete(final U result) {
    return new ImmediateCompleted<>(caller, result);
  }

  @Override
  public String toString() {
    return MessageFormat.format("{0}({1}: result={2})", getClass().getSimpleName(), Stage.COMPLETED,
        result);
  }
}
