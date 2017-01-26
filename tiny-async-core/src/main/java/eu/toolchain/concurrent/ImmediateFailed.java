package eu.toolchain.concurrent;

import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A stage which has already failed.
 *
 * @param <T> type of the completable
 */
@EqualsAndHashCode(of = {"cause"}, doNotUseGetters = true, callSuper = false)
public class ImmediateFailed<T> extends AbstractImmediate<T> implements Stage<T> {
  private final Throwable cause;

  public ImmediateFailed(
      Caller caller, Throwable cause
  ) {
    super(caller);
    this.cause = cause;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public Stage<T> handle(Handle<? super T> handle) {
    caller.execute(() -> handle.failed(cause));
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
    return this;
  }

  @Override
  public Stage<T> whenFailed(Consumer<? super Throwable> consumer) {
    caller.execute(() -> consumer.accept(cause));
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
  public <U> Stage<U> thenApply(Function<? super T, ? extends U> fn) {
    return new ImmediateFailed<>(caller, cause);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> Stage<U> thenCompose(
      Function<? super T, ? extends Stage<U>> fn
  ) {
    return new ImmediateFailed<>(caller, cause);
  }

  @Override
  public Stage<T> thenApplyFailed(Function<? super Throwable, ? extends T> fn) {
    return thenApplyCaughtFailed(fn, cause);
  }

  @Override
  public Stage<T> thenComposeCaught(
      Function<? super Throwable, ? extends Stage<T>> fn
  ) {
    return thenComposeFailedFailed(fn, cause);
  }

  @Override
  public Stage<T> withCloser(
      final Supplier<? extends Stage<Void>> complete, final Supplier<? extends Stage<Void>> notComplete
  ) {
    return withCloserFailed(cause, notComplete);
  }

  @Override
  public Stage<T> withComplete(
      final Supplier<? extends Stage<Void>> supplier
  ) {
    return this;
  }

  @Override
  public Stage<T> withNotComplete(
      final Supplier<? extends Stage<Void>> supplier
  ) {
    return withNotCompleteFailed(cause, supplier);
  }

  @Override
  public <U> Stage<U> thenFail(final Throwable cause) {
    final ExecutionException c = new ExecutionException(cause);
    c.addSuppressed(this.cause);
    return new ImmediateFailed<>(caller, c);
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
    return MessageFormat.format("{0}({1}: cause={2})", getClass().getSimpleName(), Stage.COMPLETED,
        cause);
  }
}
