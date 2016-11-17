package eu.toolchain.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A completable which has already failed.
 *
 * @param <T> type of the completable
 */
@EqualsAndHashCode(of = {"cause"}, doNotUseGetters = true, callSuper = false)
@ToString(of = {"cause"})
public class ImmediateFailed<T> extends AbstractImmediate<T> implements Stage<T> {
  private final Caller caller;
  private final Throwable cause;

  public ImmediateFailed(
      Caller caller, Throwable cause
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
  public Stage<T> handle(Handle<? super T> handle) {
    caller.execute(() -> handle.failed(cause));
    return this;
  }

  @Override
  public <U> Stage<U> applyHandle(final ApplyHandle<? super T, ? extends U> handle) {
    try {
      return new ImmediateCompleted<>(caller, handle.failed(cause));
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
  }

  @Override
  public <U> Stage<U> composeHandle(
      final ApplyHandle<? super T, ? extends Stage<U>> handle
  ) {
    try {
      return handle.failed(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      return new ImmediateFailed<>(caller, e);
    }
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
    return immediateCatchFailed(fn, cause);
  }

  @Override
  public Stage<T> thenComposeFailed(
      Function<? super Throwable, ? extends Stage<T>> fn
  ) {
    return immediateComposeFailed(fn, cause);
  }

  @Override
  public Stage<T> thenApplyCancelled(Supplier<? extends T> supplier) {
    return this;
  }

  @Override
  public Stage<T> thenComposeCancelled(Supplier<? extends Stage<T>> supplier) {
    return this;
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
}
