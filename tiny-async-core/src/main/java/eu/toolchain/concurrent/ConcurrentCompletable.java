package eu.toolchain.concurrent;

import java.text.MessageFormat;
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
import lombok.RequiredArgsConstructor;

/**
 * A concurrent implementation of {@link Completable}.
 *
 * <p>The callback uses the calling thread to execute result listeners, see
 * {@link #postComplete()} for details.
 *
 * @param <T> type of the completable stage
 */
public class ConcurrentCompletable<T> extends AbstractImmediate<T>
    implements Handle<T>, Completable<T> {
  /**
   * the max number of spins allowed before {@link Thread#yield()}
   */
  static final int MAX_SPINS = 4;

  /**
   * Possible states of the completable. A state being set <em>does not</em> indicate that the
   * completable is no longer running, but must always be checked in concert with a {@code result !=
   * null}.
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

  private final Caller caller;

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
  final AtomicReference<RunnablePair> callbacks;

  /**
   * Setup a concurrent completable that uses a custom caller implementation. <p> The provided
   * caller implementation will be called from the calling thread of {@link #handle(Handle)}, or
   * other public methods interacting with this completable. <p> It is therefore suggested to
   * provide an implementation that supports delegating tasks to a separate thread pool.
   *
   * @param caller The caller implementation to use.
   */
  public ConcurrentCompletable(final Caller caller) {
    super(caller);
    this.caller = caller;
    this.callbacks = new AtomicReference<>();
  }

  /**
   * Constructor that provides an initial callback.
   *
   * @param caller caller implementation to use
   * @param runnable initial callback
   */
  ConcurrentCompletable(final Caller caller, final Runnable runnable) {
    super(caller);
    this.caller = caller;
    this.callbacks = new AtomicReference<>(new RunnablePair(runnable, null));
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
  public Stage<T> handle(final Handle<? super T> handle) {
    final Runnable runnable = handleRunnable(handle);

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
  public Stage<T> whenDone(final Runnable finishable) {
    if (add(finishable)) {
      return this;
    }

    caller.execute(finishable);
    return this;
  }

  @Override
  public Stage<T> whenComplete(final Consumer<? super T> consumer) {
    final Runnable runnable = completedRunnable(consumer);

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

    return throwable(result);
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
        case COMPLETED:
          return thenApplyCompleted(fn, result(r));
        case FAILED:
          return new ImmediateFailed<>(caller, throwable(r));
        default:
          return new ImmediateCancelled<>(caller);
      }
    }

    final ConcurrentCompletable<U> target = nextStage();
    whenDone(new ThenApply<>(target, fn));
    return target;
  }

  @Override
  public <U> Stage<U> thenCompose(final Function<? super T, ? extends Stage<U>> fn) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case COMPLETED:
          return thenComposeCompleted(fn, result(r));
        case FAILED:
          return new ImmediateFailed<>(caller, throwable(r));
        default:
          return new ImmediateCancelled<>(caller);
      }
    }

    final ConcurrentCompletable<U> target = nextStage();
    whenDone(new ThenCompose<>(target, fn));
    return target;
  }

  @Override
  public Stage<T> thenApplyFailed(final Function<? super Throwable, ? extends T> fn) {
    final Object r = result;

    if (r != null) {
      if (state.get() == FAILED) {
        return thenApplyCaughtFailed(fn, throwable(r));
      }

      return this;
    }

    final ConcurrentCompletable<T> target = nextStage();
    whenDone(new ThenApplyFailed(target, fn));
    return target;
  }

  @Override
  public Stage<T> thenComposeCaught(final Function<? super Throwable, ? extends Stage<T>> fn) {
    final Object r = result;

    if (r != null) {
      if (state.get() == FAILED) {
        return thenComposeFailedFailed(fn, throwable(r));
      }

      return this;
    }

    final ConcurrentCompletable<T> target = nextStage();
    whenDone(new ThenComposeFailed(target, fn));
    return target;
  }

  @Override
  public Stage<T> withCloser(
      final Supplier<? extends Stage<Void>> complete, final Supplier<? extends Stage<Void>> other
  ) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case COMPLETED:
          return withCloserCompleted(result(result), complete, other);
        case FAILED:
          return withCloserFailed(throwable(result), other);
        default:
          return withCloserCancelled(other);
      }
    }

    final ConcurrentCompletable<T> target = nextStage();
    whenDone(new WithCloser(target, complete, other));
    return target;
  }

  @Override
  public Stage<T> withComplete(final Supplier<? extends Stage<Void>> supplier) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case COMPLETED:
          return withCompleteCompleted(result(result), supplier);
        case FAILED:
          return new ImmediateFailed<>(caller, throwable(result));
        default:
          return new ImmediateCancelled<>(caller);
      }
    }

    final ConcurrentCompletable<T> target = nextStage();
    whenDone(new WithComplete(target, supplier));
    return target;
  }

  @Override
  public Stage<T> withOther(
      final Supplier<? extends Stage<Void>> supplier
  ) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case COMPLETED:
          return new ImmediateCompleted<>(caller, result(result));
        case FAILED:
          return withNotCompleteFailed(throwable(result), supplier);
        default:
          return withNotCompleteCancelled(supplier);
      }
    }

    final ConcurrentCompletable<T> target = nextStage();
    whenDone(new WithNotComplete(target, supplier));
    return target;
  }

  @Override
  public <U> Stage<U> thenFail(final Throwable cause) {
    final Object r = result;

    if (r != null) {
      switch (state.get()) {
        case FAILED:
          final ExecutionException c = new ExecutionException(cause);
          c.addSuppressed(throwable(r));
          return new ImmediateFailed<>(caller, c);
        default:
          return new ImmediateFailed<>(caller, cause);
      }
    }

    final ConcurrentCompletable<U> target = nextStage();
    whenDone(new ThenFail<>(target, cause));
    return target;
  }

  @Override
  public <U> Stage<U> thenComplete(final U result) {
    return thenApply(v -> result);
  }

  @Override
  public <U> Stage<U> thenCancel() {
    final Object r = result;

    if (r != null) {
      return new ImmediateCancelled<>(caller);
    }

    final ConcurrentCompletable<U> target = nextStage();
    whenDone(target::cancel);
    return target;
  }

  <U> ConcurrentCompletable<U> nextStage() {
    return new ConcurrentCompletable<>(caller, this::cancel);
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
      case COMPLETED:
        return result(r);
      case FAILED:
        throw new ExecutionException(throwable(r));
      default:
        throw new CancellationException();
    }
  }

  Runnable handleRunnable(final Handle<? super T> done) {
    return () -> {
      switch (state.get()) {
        case COMPLETED:
          try {
            done.completed(result(result));
          } catch (final Exception e) {
            done.failed(e);
          }

          break;
        case FAILED:
          done.failed(throwable(result));
          break;
        default:
          try {
            done.cancelled();
          } catch (final Exception e) {
            done.failed(e);
          }

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

  Runnable completedRunnable(final Consumer<? super T> consumer) {
    return () -> {
      if (state.get() == COMPLETED) {
        consumer.accept(result(result));
      }
    };
  }

  Runnable failedRunnable(final Consumer<? super Throwable> consumer) {
    return () -> {
      if (state.get() == FAILED) {
        consumer.accept(throwable(result));
      }
    };
  }

  @Override
  public String toString() {
    final String name = getClass().getSimpleName();

    if (this.result == null) {
      return MessageFormat.format("{0}({1})", name, Stage.PENDING);
    }

    switch (state.get()) {
      case COMPLETED:
        return MessageFormat.format("{0}({1}: result={2})", name, Stage.COMPLETED,
            result(this.result));
      case FAILED:
        return MessageFormat.format("{0}({1}: cause={2})", name, Stage.FAILED,
            throwable(this.result));
      default:
        return MessageFormat.format("{0}({1})", name, Stage.CANCELLED);
    }
  }

  /**
   * Convert the result object to a result.
   *
   * <p>Takes {@link #NULL} into account.
   *
   * @param r the result object
   * @return result
   */
  @SuppressWarnings("unchecked")
  T result(final Object r) {
    return (T) (r == NULL ? null : r);
  }

  /**
   * Convert the result object to a Throwable.
   *
   * @param r result object
   * @return throwable
   */
  static Throwable throwable(final Object r) {
    return (Throwable) r;
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

  <U> void handleStage(
      final Supplier<? extends Stage<U>> supplier, final ConcurrentCompletable<U> target
  ) {
    final Stage<U> next;

    try {
      next = supplier.get();
    } catch (final Exception e) {
      target.fail(e);
      return;
    }

    final Stage<U> n = next.handle(target);
    target.whenCancelled(n::cancel);
  }

  @RequiredArgsConstructor
  class ThenApply<U> implements Runnable {
    private final ConcurrentCompletable<U> target;
    private final Function<? super T, ? extends U> fn;

    @Override
    public void run() {
      switch (state.get()) {
        case COMPLETED:
          final U r;

          try {
            r = fn.apply(result(result));
          } catch (final Exception e) {
            target.fail(e);
            return;
          }

          target.complete(r);
          break;
        case FAILED:
          target.fail(throwable(result));
          break;
        default:
          target.cancel();
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class ThenCompose<U> implements Runnable {
    private final ConcurrentCompletable<U> target;
    private final Function<? super T, ? extends Stage<U>> fn;

    @Override
    public void run() {
      switch (state.get()) {
        case COMPLETED:
          handleStage(() -> fn.apply(result(result)), target);
          break;
        case FAILED:
          target.fail(throwable(result));
          break;
        default:
          target.cancel();
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class ThenApplyFailed implements Runnable {
    private final ConcurrentCompletable<T> target;
    private final Function<? super Throwable, ? extends T> fn;

    @Override
    public void run() {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          final T r;

          try {
            r = fn.apply(throwable(result));
          } catch (final Exception e) {
            target.fail(e);
            return;
          }

          target.complete(r);
          break;
        default:
          target.cancel();
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class ThenComposeFailed implements Runnable {
    private final ConcurrentCompletable<T> target;
    private final Function<? super Throwable, ? extends Stage<T>> fn;

    @Override
    public void run() {
      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          handleStage(() -> fn.apply(throwable(result)), target);
          break;
        default:
          target.cancel();
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class WithCloser implements Runnable {
    private final ConcurrentCompletable<T> target;
    private final Supplier<? extends Stage<Void>> complete;
    private final Supplier<? extends Stage<Void>> notComplete;

    @Override
    public void run() {
      final Stage<Void> next;

      switch (state.get()) {
        case COMPLETED:
          try {
            next = complete.get();
          } catch (final Exception e) {
            notComplete.get().whenDone(() -> target.fail(e));
            return;
          }

          target.whenCancelled(next::cancel);
          next.thenApply(v -> result(result)).handle(target);
          break;
        case FAILED:
          try {
            next = notComplete.get();
          } catch (final Exception e) {
            final ExecutionException ee = new ExecutionException(e);
            ee.addSuppressed(throwable(result));
            target.fail(ee);
            return;
          }

          target.whenCancelled(next::cancel);
          next.<T>thenFail(throwable(result)).handle(target);
          break;
        default:
          try {
            next = notComplete.get();
          } catch (final Exception e) {
            target.fail(e);
            return;
          }

          target.whenCancelled(next::cancel);
          next.<T>thenCancel().handle(target);
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class WithComplete implements Runnable {
    private final ConcurrentCompletable<T> target;
    private final Supplier<? extends Stage<Void>> complete;

    @Override
    public void run() {
      final Stage<Void> next;

      switch (state.get()) {
        case COMPLETED:
          try {
            next = complete.get();
          } catch (final Exception e) {
            target.fail(e);
            return;
          }

          target.whenCancelled(next::cancel);
          next.thenApply(v -> result(result)).handle(target);
          break;
        case FAILED:
          target.fail(throwable(result));
          break;
        default:
          target.cancel();
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class WithNotComplete implements Runnable {
    private final ConcurrentCompletable<T> target;
    private final Supplier<? extends Stage<Void>> notComplete;

    @Override
    public void run() {
      final Stage<Void> next;

      switch (state.get()) {
        case COMPLETED:
          target.complete(result(result));
          break;
        case FAILED:
          try {
            next = notComplete.get();
          } catch (final Exception e) {
            final ExecutionException ee = new ExecutionException(e);
            ee.addSuppressed(throwable(result));
            target.fail(ee);
            return;
          }

          target.whenCancelled(next::cancel);
          next.<T>thenFail(throwable(result)).handle(target);
          break;
        default:
          try {
            next = notComplete.get();
          } catch (final Exception e) {
            target.fail(e);
            return;
          }

          target.whenCancelled(next::cancel);
          next.<T>thenCancel().handle(target);
          break;
      }
    }
  }

  @RequiredArgsConstructor
  class ThenFail<U> implements Runnable {
    private final ConcurrentCompletable<U> target;
    private final Throwable cause;

    @Override
    public void run() {
      switch (state.get()) {
        case FAILED:
          final ExecutionException c = new ExecutionException(cause);
          c.addSuppressed(throwable(result));
          target.fail(cause);
          break;
        default:
          target.fail(cause);
          break;
      }
    }
  }
}
