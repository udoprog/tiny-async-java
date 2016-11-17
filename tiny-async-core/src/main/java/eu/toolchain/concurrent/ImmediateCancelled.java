package eu.toolchain.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A completable which has already been cancelled.
 *
 * @param <T> type of the completable
 */
@EqualsAndHashCode(of = {}, doNotUseGetters = true, callSuper = false)
@ToString(of = {})
public class ImmediateCancelled<T> extends AbstractImmediate<T> implements Stage<T> {
  private final Caller caller;

  public ImmediateCancelled(final Caller caller) {
    super(caller);
    this.caller = caller;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public Stage<T> handle(Handle<? super T> handle) {
    caller.execute(handle::cancelled);
    return this;
  }

  @Override
  public <U> Stage<U> applyHandle(final ApplyHandle<? super T, ? extends U> handle) {
    try {
      return new ImmediateCompleted<>(caller, handle.cancelled());
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }
  }

  @Override
  public <U> Stage<U> composeHandle(
      final ApplyHandle<? super T, ? extends Stage<U>> handle
  ) {
    try {
      return handle.cancelled();
    } catch (final Exception e) {
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
    caller.execute(runnable);
    return this;
  }

  @Override
  public Stage<T> whenComplete(Consumer<? super T> consumer) {
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
    throw new IllegalStateException("completable is not in a failed state");
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

  @Override
  public <U> Stage<U> thenApply(Function<? super T, ? extends U> fn) {
    return new ImmediateCancelled<>(caller);
  }

  @Override
  public <U> Stage<U> thenCompose(
      Function<? super T, ? extends Stage<U>> fn
  ) {
    return new ImmediateCancelled<>(caller);
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
    return immediateCatchCancelled(supplier);
  }

  @Override
  public Stage<T> thenComposeCancelled(Supplier<? extends Stage<T>> supplier) {
    return immediateComposeCancelled(supplier);
  }

  @Override
  public Stage<T> withCloser(
      final Supplier<? extends Stage<Void>> complete,
      final Supplier<? extends Stage<Void>> notComplete
  ) {
    final Stage<Void> next;

    try {
      next = notComplete.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return next.thenCancel();
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
    return supplier.get().thenCancel();
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
}
