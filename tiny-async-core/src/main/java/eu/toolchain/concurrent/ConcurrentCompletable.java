package eu.toolchain.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

/**
 * A thread-safe implementation of Completable.
 * <p>
 * The callback uses the calling thread to execute result listeners, see {@link #complete(Object)},
 * and {@link #whenDone(eu.toolchain.concurrent.CompletionHandle)} for details.
 *
 * @param <T> type of the completable
 * @author udoprog
 */
public class ConcurrentCompletable<T> extends AbstractImmediate<T>
    implements CompletionHandle<T>, Completable<T> {
  /**
   * the max number of spins allowed before {@link Thread#yield()}
   */
  static final int MAX_SPINS = 4;

  /**
   * Possible states of the completable.
   * A state being set <em>does not</em> indicate that the completable is no longer running, but must
   * always be checked in concert with a {@code result != null}.
   */
  static final int PENDING = 0;
  static final int COMPLETED = 1;
  static final int FAILED = 2;
  static final int CANCELLED = 3;

  /**
   * Indicates a result containing {@code null}.
   */
  static final Object NULL = new Object();
  static final Object CANCEL = new Object();

  /**
   * pair to CAS into callbacks when done
   */
  static final RunnablePair END = new RunnablePair(null, null);

  private final FutureCaller caller;

  /**
   * Current state of the completable.
   */
  final AtomicInteger state = new AtomicInteger();
  /**
   * Result of the completable.
   * Never stored null, but uses {@link #NULL} as a surrogate instead.
   */
  volatile Object result = null;
  /**
   * a linked list of callbacks to execute
   */
  final AtomicReference<RunnablePair> callbacks = new AtomicReference<>();

  /**
   * Setup a concurrent completable that uses a custom caller implementation. <p> The provided caller
   * implementation will be called from the calling thread of
   * {@link #whenDone(eu.toolchain.concurrent.CompletionHandle)},
   * or other public methods interacting with this completable. <p> It is therefore suggested to provide
   * an implementation that supports delegating tasks to a separate thread pool.
   *
   * @param caller The caller implementation to use.
   */
  public ConcurrentCompletable(final FutureCaller caller) {
    super(caller);
    this.caller = caller;
  }

  @Override
  public void completed(final T result) {
    complete(result);
  }

  @Override
  public void failed(final Throwable cause) {
    fail(cause);
  }

  @Override
  public void cancelled() {
    cancel();
  }

  @Override
  public boolean complete(final T result) {
    if (!state.compareAndSet(PENDING, COMPLETED)) {
      return false;
    }

    this.result = (result == null ? NULL : result);
    postComplete();
    return true;
  }

  @Override
  public boolean fail(final Throwable cause) {
    if (cause == null) {
      throw new IllegalArgumentException("cause");
    }

    if (!state.compareAndSet(PENDING, FAILED)) {
      return false;
    }

    this.result = cause;
    postComplete();
    return true;
  }

  @Override
  public boolean cancel() {
    if (!state.compareAndSet(PENDING, CANCELLED)) {
      return false;
    }

    this.result = CANCEL;
    postComplete();
    return true;
  }

  @Override
  public Stage<T> whenDone(final CompletionHandle<? super T> handle) {
    final Runnable runnable = doneRunnable(handle);

    if (add(runnable)) {
      return this;
    }

    caller.execute(runnable);
    return this;
  }

  @Override
  public Stage<T> whenCancelled(final Runnable cancelled) {
    final Runnable runnable = cancelledRunnable(cancelled);

    if (add(runnable)) {
      return this;
    }

    caller.execute(runnable);
    return this;
  }

  @Override
  public Stage<T> whenFinished(final Runnable finishable) {
    if (add(finishable)) {
      return this;
    }

    caller.execute(finishable);
    return this;
  }

  @Override
  public Stage<T> whenComplete(final Consumer<? super T> consumer) {
    final Runnable runnable = resolvedRunnable(consumer);

    if (add(runnable)) {
      return this;
    }

    caller.execute(runnable);
    return this;
  }

  @Override
  public Stage<T> whenFailed(final Consumer<? super Throwable> consumer) {
    final Runnable runnable = failedRunnable(consumer);

    if (add(runnable)) {
      return this;
    }

    caller.execute(runnable);
    return this;
  }

  @Override
  public boolean isDone() {
    return result != null;
  }

  @Override
  public boolean isCompleted() {
    return result != null && state.get() == COMPLETED;
  }

  @Override
  public boolean isFailed() {
    return result != null && state.get() == FAILED;
  }

  @Override
  public boolean isCancelled() {
    return result == CANCEL;
  }

  @Override
  public Throwable cause() {
    if (state.get() != FAILED) {
      throw new IllegalStateException("not in a failed state");
    }

    return (Throwable) result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T join() throws InterruptedException, ExecutionException {
    final Parker parker = new Parker(Thread.currentThread());

    if (add(parker)) {
      parker.park();
    }

    return doJoin();
  }

  @Override
  public T joinNow() throws ExecutionException {
    return doJoin();
  }

  @Override
  public T join(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    final long timeoutNanos = unit.toNanos(timeout);

    if (timeoutNanos <= 0L) {
      throw new TimeoutException();
    }

    final Parker parker = new Parker(Thread.currentThread());

    /* attempt to schedule an unpark for later,
     * this will happen after result is available */
    if (add(parker)) {
      parker.parkNanos(timeoutNanos);
    }

    return doJoin();
  }

  @Override
  public <U> Stage<U> thenApply(Function<? super T, ? extends U> fn) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case CANCELLED:
          return new ImmediateCancelled<>(caller);
        case FAILED:
          return new ImmediateFailed<>(caller, (Throwable) r);
        default:
          return immediateApply(fn, result(r));
      }
    }

    final Completable<U> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          try {
            target.complete(fn.apply(result(result)));
          } catch (Exception e) {
            target.fail(e);
          }
          break;
        case FAILED:
          target.fail((Throwable) result);
          break;
        default:
          target.cancel();
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  @Override
  public <U> Stage<U> thenCompose(
      final Function<? super T, ? extends Stage<U>> fn
  ) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case CANCELLED:
          return new ImmediateCancelled<>(caller);
        case FAILED:
          return new ImmediateFailed<>(caller, (Throwable) r);
        default:
          return immediateCompose(fn, result(r));
      }
    }

    final ConcurrentCompletable<U> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          try {
            target.whenCancelled(fn.apply(result(result)).whenDone(target)::cancel);
          } catch (final Exception e) {
            target.fail(e);
          }
          break;
        case FAILED:
          target.fail((Throwable) result);
          break;
        default:
          target.cancel();
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  @Override
  public Stage<T> thenApplyFailed(
      final Function<? super Throwable, ? extends T> fn
  ) {
    final Object r = result;

    if (r != null) {
      if (state.get() == FAILED) {
        return immediateCatchFailed(fn, (Throwable) r);
      }

      return this;
    }

    final Completable<T> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          try {
            target.complete(fn.apply((Throwable) result));
          } catch (Exception e) {
            target.fail(e);
          }
          break;
        default:
          target.cancel();
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  @Override
  public Stage<T> thenComposeFailed(
      Function<? super Throwable, ? extends Stage<T>> fn
  ) {
    final Object r = result;

    if (r != null) {
      if (state.get() == FAILED) {
        return immediateComposeFailed(fn, (Throwable) r);
      }

      return this;
    }

    final ConcurrentCompletable<T> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          try {
            target.whenCancelled(fn.apply((Throwable) result).whenDone(target)::cancel);
          } catch (final Exception e) {
            target.fail(e);
          }
          break;
        default:
          target.cancel();
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  @Override
  public Stage<T> thenApplyCancelled(final Supplier<? extends T> supplier) {
    if (result != null) {
      if (state.get() == CANCELLED) {
        return immediateCatchCancelled(supplier);
      }

      return this;
    }

    final Completable<T> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          target.fail((Throwable) result);
          break;
        default:
          try {
            target.complete(supplier.get());
          } catch (final Exception e) {
            target.fail(e);
          }
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  @Override
  public Stage<T> thenComposeCancelled(
      Supplier<? extends Stage<T>> supplier
  ) {
    final Object r = result;

    if (r != null) {
      if (state.get() == CANCELLED) {
        return immediateComposeCancelled(supplier);
      }

      return this;
    }

    final ConcurrentCompletable<T> target = newFuture();

    whenFinished(() -> {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          target.fail((Throwable) result);
          break;
        default:
          try {
            target.whenCancelled(supplier.get().whenDone(target)::cancel);
          } catch (final Exception e) {
            target.fail(e);
          }
          break;
      }
    });

    return target.whenCancelled(this::cancel);
  }

  <U> ConcurrentCompletable<U> newFuture() {
    return new ConcurrentCompletable<>(caller);
  }

  /**
   * Convert an Object into a result.
   * <p>
   * Takes {@link #NULL} into account.
   *
   * @param r object that contains the result
   * @param <T> type of result
   * @return result
   */
  @SuppressWarnings("unchecked")
  static <T> T result(final Object r) {
    return (T) (r == NULL ? null : r);
  }

  void postComplete() {
    RunnablePair entries = takeAndClear();

    while (entries != null) {
      caller.execute(entries.runnable);
      entries = entries.next;
    }
  }

  /**
   * Take and reset all callbacks in an atomic fashion.
   */
  RunnablePair takeAndClear() {
    RunnablePair entries;

    while ((entries = callbacks.get()) != END) {
      if (callbacks.compareAndSet(entries, END)) {
        return entries;
      }
    }

    return null;
  }

  /**
   * Attempt to add an event listener to the list of listeners.
   * <p>
   * This implementation uses a spin-lock, where the loop copies the entire list of listeners.
   *
   * @return {@code true} if a task has been queued up, {@code false} otherwise.
   */
  boolean add(Runnable runnable) {
    int spins = 0;

    RunnablePair entries;

    while ((entries = callbacks.get()) != END) {
      if (callbacks.compareAndSet(entries, new RunnablePair(runnable, entries))) {
        return true;
      }

      if (spins++ > MAX_SPINS) {
        Thread.yield();
        spins = 0;
      }
    }

    return false;
  }

  T doJoin() throws ExecutionException {
    final Object r = this.result;

    if (r == null) {
      throw new IllegalStateException("result is not available");
    }

    switch (state.get()) {
      case CANCELLED:
        throw new CancellationException();
      case FAILED:
        throw new ExecutionException((Throwable) r);
      default:
        return result(r);
    }
  }

  Runnable doneRunnable(final CompletionHandle<? super T> done) {
    return () -> {
      switch (state.get()) {
        case FAILED:
          done.failed((Throwable) result);
          break;
        case CANCELLED:
          done.cancelled();
          break;
        default:
          done.completed(result(result));
          break;
      }
    };
  }

  Runnable cancelledRunnable(final Runnable cancelled) {
    return () -> {
      if (state.get() == CANCELLED) {
        cancelled.run();
      }
    };
  }

  Runnable resolvedRunnable(final Consumer<? super T> consumer) {
    return () -> {
      if (state.get() == COMPLETED) {
        consumer.accept(result(result));
      }
    };
  }

  Runnable failedRunnable(final Consumer<? super Throwable> consumer) {
    return () -> {
      if (state.get() == FAILED) {
        consumer.accept((Throwable) result);
      }
    };
  }

  /**
   * A single node in a list of runnables that should be executed when done.
   */
  @AllArgsConstructor
  static class RunnablePair {
    final Runnable runnable;
    final RunnablePair next;
  }

  @AllArgsConstructor
  class Parker implements Runnable {
    /**
     * Thread to unpark.
     * <p>
     * Is set to null to avoid unparking it when no longer needed.
     */
    volatile Thread thread;

    @Override
    public void run() {
      final Thread t = thread;

      if (t != null) {
        thread = null;
        LockSupport.unpark(t);
      }
    }

    void parkNanos(final long nanos) throws InterruptedException, TimeoutException {
      final long deadline = System.nanoTime() + nanos;

      while (result == null) {
        if (Thread.interrupted()) {
          thread = null;
          throw new InterruptedException();
        }

        final long parkNanos = deadline - System.nanoTime();

        if (parkNanos <= 0) {
          thread = null;
          throw new TimeoutException();
        }

        LockSupport.parkNanos(this, parkNanos);
      }
    }

    void park() throws InterruptedException {
      while (result == null) {
        if (Thread.interrupted()) {
          thread = null;
          throw new InterruptedException();
        }

        LockSupport.park(this);
      }
    }
  }
}
