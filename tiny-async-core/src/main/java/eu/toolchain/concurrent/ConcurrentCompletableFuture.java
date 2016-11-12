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
 * A thread-safe implementation of CompletableFuture.
 * <p>
 * The callback uses the calling thread to execute result listeners, see {@link #complete(Object)},
 * and {@link #handle(eu.toolchain.concurrent.CompletionHandle)} for details.
 *
 * @param <T> type of the future
 * @author udoprog
 */
public class ConcurrentCompletableFuture<T> extends AbstractImmediate<T>
    implements CompletableFuture<T> {
  /**
   * the max number of spins allowed before {@link Thread#yield()}
   */
  static final int MAX_SPINS = 4;

  /**
   * Possible states of the future.
   * A state being set <em>does not</em> indicate that the future is no longer running, but must
   * always be checked in concert with a {@code result != null}.
   */
  static final int RUNNING = 0;
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

  /**
   * a linked list of callbacks to execute
   */
  final AtomicReference<RunnablePair> callbacks = new AtomicReference<>();
  /**
   * Current state of the future.
   */
  final AtomicInteger state = new AtomicInteger();
  /**
   * Result of the future.
   * Never stored null, but uses {@link #NULL} as a surrogate instead.
   */
  volatile Object result = null;

  private final FutureCaller caller;

  /**
   * Setup a concurrent future that uses a custom caller implementation.
   * <p>
   * The provided caller implementation will be called from the calling thread of {@link
   * #handle(eu.toolchain.concurrent.CompletionHandle)}, or other public methods interacting with
   * this future.
   * <p>
   * It is therefore suggested to provide an implementation that supports delegating tasks to a
   * separate thread pool.
   *
   * @param caller The caller implementation to use.
   */
  public ConcurrentCompletableFuture(final FutureCaller caller) {
    super(caller);
    this.caller = caller;
  }

  @Override
  public boolean complete(final T result) {
    if (!state.compareAndSet(RUNNING, COMPLETED)) {
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

    if (!state.compareAndSet(RUNNING, FAILED)) {
      return false;
    }

    this.result = cause;
    postComplete();
    return true;
  }

  @Override
  public boolean cancel() {
    if (!state.compareAndSet(RUNNING, CANCELLED)) {
      return false;
    }

    this.result = CANCEL;
    postComplete();
    return true;
  }

  @Override
  public CompletionStage<T> bind(final CompletionStage<?> other) {
    final Runnable runnable = otherRunnable(other);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
    return this;
  }

  @Override
  public CompletionStage<T> handle(final CompletionHandle<? super T> handle) {
    final Runnable runnable = doneRunnable(handle);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
    return this;
  }

  @Override
  public CompletionStage<T> whenCancelled(final Runnable cancelled) {
    final Runnable runnable = cancelledRunnable(cancelled);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
    return this;
  }

  @Override
  public CompletionStage<T> whenFinished(final Runnable finishable) {
    final Runnable runnable = finishedRunnable(finishable);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
    return this;
  }

  @Override
  public CompletionStage<T> whenCompleted(final Consumer<? super T> consumer) {
    final Runnable runnable = resolvedRunnable(consumer);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
    return this;
  }

  @Override
  public CompletionStage<T> whenFailed(final Consumer<? super Throwable> consumer) {
    final Runnable runnable = failedRunnable(consumer);

    if (add(runnable)) {
      return this;
    }

    runnable.run();
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

    /* transform */

  @Override
  public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<U> target = newFuture();
      handle(new ThenApplyHelper<>(fn, target));
      return target.bind(this);
    }

    final int s = state.get();

    if (s == CANCELLED) {
      return new ImmediateCancelled<>(caller);
    }

    if (s == FAILED) {
      return new ImmediateFailed<>(caller, (Throwable) r);
    }

    return immediateApply(fn, result(r));
  }

  @Override
  public <U> CompletionStage<U> thenCompose(
      final Function<? super T, ? extends CompletionStage<U>> fn
  ) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<U> target = newFuture();
      handle(new ThenComposeHelper<>(fn, target));
      return target.bind(this);
    }

    final int s = state.get();

    if (s == CANCELLED) {
      return new ImmediateCancelled<>(caller);
    }

    if (s == FAILED) {
      return new ImmediateFailed<>(caller, (Throwable) r);
    }

    return immediateCompose(fn, result(r));
  }

  @Override
  public CompletionStage<T> thenCatchFailed(
      Function<? super Throwable, ? extends T> fn
  ) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<T> target = newFuture();
      handle(new ThenCatchFailedHelper<>(fn, target));
      return target.bind(this);
    }

    if (state.get() == FAILED) {
      return immediateCatchFailed(fn, (Throwable) r);
    }

    return this;
  }

  @Override
  public CompletionStage<T> thenComposeFailed(
      Function<? super Throwable, ? extends CompletionStage<T>> fn
  ) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<T> target = newFuture();
      handle(new ThenComposeFailedHelper<>(fn, target));
      return target.bind(this);
    }

    if (state.get() == FAILED) {
      return immediateComposeFailed(fn, (Throwable) r);
    }

    return this;
  }

  @Override
  public CompletionStage<T> thenCatchCancelled(Supplier<? extends T> supplier) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<T> target = newFuture();
      handle(new ThenCatchCancelledHelper<>(supplier, target));
      return target.bind(this);
    }

    if (state.get() == CANCELLED) {
      return immediateCatchCancelled(supplier);
    }

    return this;
  }

  @Override
  public CompletionStage<T> thenComposeCancelled(
      Supplier<? extends CompletionStage<T>> supplier
  ) {
    final Object r = result;

    if (r == null) {
      final CompletableFuture<T> target = newFuture();
      handle(new TheComposeCancelledHelper<>(supplier, target));
      return target.bind(this);
    }

    if (state.get() == CANCELLED) {
      return immediateComposeCancelled(supplier);
    }

    return this;
  }

  <U> CompletableFuture<U> newFuture() {
    return new ConcurrentCompletableFuture<>(caller);
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
      entries.runnable.run();
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

    final int s = state.get();

    if (s == CANCELLED) {
      throw new CancellationException();
    }

    if (s == FAILED) {
      throw new ExecutionException((Throwable) r);
    }

    return result(result);
  }

  Runnable otherRunnable(final CompletionStage<?> other) {
    return () -> {
      if (state.get() == CANCELLED) {
        other.cancel();
      }
    };
  }

  Runnable doneRunnable(final CompletionHandle<? super T> done) {
    return () -> {
      final int s = state.get();

      if (s == FAILED) {
        caller.fail(done, (Throwable) result);
        return;
      }

      if (s == CANCELLED) {
        caller.cancel(done);
        return;
      }

      caller.complete(done, result(result));
    };
  }

  Runnable cancelledRunnable(final Runnable cancelled) {
    return () -> {
      if (state.get() == CANCELLED) {
        caller.cancel(cancelled);
      }
    };
  }

  Runnable finishedRunnable(final Runnable finishable) {
    return () -> caller.finish(finishable);
  }

  Runnable resolvedRunnable(final Consumer<? super T> resolved) {
    return () -> {
      if (state.get() == COMPLETED) {
        caller.complete(resolved, result(result));
      }
    };
  }

  Runnable failedRunnable(final Consumer<? super Throwable> failed) {
    return () -> {
      if (state.get() == FAILED) {
        caller.fail(failed, (Throwable) result);
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
