package eu.toolchain.concurrent;

import eu.toolchain.concurrent.concurrent.ConcurrentCompletableFuture;
import eu.toolchain.concurrent.concurrent.ConcurrentManaged;
import eu.toolchain.concurrent.concurrent.ConcurrentReloadableManaged;
import eu.toolchain.concurrent.helper.CollectAndDiscardHelper;
import eu.toolchain.concurrent.helper.CollectHelper;
import eu.toolchain.concurrent.helper.RetryCallHelper;
import eu.toolchain.concurrent.immediate.ImmediateCancelledStage;
import eu.toolchain.concurrent.immediate.ImmediateCompletedStage;
import eu.toolchain.concurrent.immediate.ImmediateFailedStage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

// @formatter:off
/**
 * Entry point to the tiny async framework.
 *
 * <h4>Example usage</h4>
 *
 * {@code
 *   TinyFuture async = TinyFuture.builder().caller(new Slf4jCaller()).build();
 *
 *   final CompletableFuture<Integer> future = async.future()
 *
 *   someAsyncOperation(future);
 *
 *   future.on(new CompletionHandle<Integer>() {
 *     void resolved(Integer result) {
 *       // hurray
 *     }
 *
 *     void failed(Throwable cause) {
 *       // nay :(
 *     }
 *   });
 * }
 */
// @formatter:on
public class TinyFuture implements FutureFramework {
  @SuppressWarnings("unchecked")
  private static final Collection<Object> EMPTY_RESULTS = Collections.EMPTY_LIST;

  /**
   * Default executor to use when resolving asynchronously.
   */
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduler;
  /**
   * Default set of helper functions for calling callbacks.
   */
  private final FutureCaller caller;
  private final ClockSource clockSource;

  protected TinyFuture(
      ExecutorService executor, ScheduledExecutorService scheduler, FutureCaller caller,
      ClockSource clockSource
  ) {
    if (caller == null) {
      throw new NullPointerException("caller");
    }

    this.executor = executor;
    this.scheduler = scheduler;
    this.caller = caller;
    this.clockSource = clockSource;
  }

  /**
   * Fetch the configured primary executor (if any).
   *
   * @return The configured primary executor.
   * @throws IllegalStateException if no caller executor is available.
   */
  public ExecutorService executor() {
    if (executor == null) {
      throw new IllegalStateException("no default executor configured");
    }

    return executor;
  }

  public FutureCaller caller() {
    return caller;
  }

  @Override
  public <C> CompletionStage<C> call(final Callable<? extends C> callable) {
    return doCall(callable, executor(), this.future());
  }

  @Override
  public <C> CompletionStage<C> call(
      final Callable<? extends C> callable, final ExecutorService executor
  ) {
    return doCall(callable, executor, this.future());
  }

  public <C> CompletionStage<C> doCall(
      final Callable<? extends C> callable, final ExecutorService executor,
      final CompletableFuture<C> future
  ) {
    final Runnable runnable = () -> {
      // future is already done, do not perform potentially expensive operation.
      if (future.isDone()) {
        return;
      }

      final C result;

      try {
        result = callable.call();
      } catch (final Exception error) {
        future.fail(error);
        return;
      }

      future.complete(result);
    };

    final Future<?> task;

    try {
      task = executor.submit(runnable);
    } catch (final Exception e) {
      future.fail(e);
      return future;
    }

    future.whenCancelled(() -> {
      // cancel, but do not interrupt.
      task.cancel(false);
    });

    return future;
  }

  @Override
  public <T> CompletableFuture<T> future() {
    return new ConcurrentCompletableFuture<>(this, caller);
  }

  @Override
  public CompletionStage<Void> completed() {
    return completed(null);
  }

  @Override
  public <T> CompletionStage<T> completed(T value) {
    return new ImmediateCompletedStage<>(this, caller, value);
  }

  @Override
  public <T> CompletionStage<T> failed(Throwable e) {
    return new ImmediateFailedStage<>(this, caller, e);
  }

  @Override
  public <T> CompletionStage<T> cancelled() {
    return new ImmediateCancelledStage<T>(this, caller);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> CompletionStage<Collection<T>> collect(
      final Collection<? extends CompletionStage<? extends T>> futures
  ) {
    if (futures.isEmpty()) {
      return completed((Collection<T>) EMPTY_RESULTS);
    }

    return collect(futures, Function.identity());
  }

  @Override
  public <C, T> CompletionStage<T> collect(
      final Collection<? extends CompletionStage<? extends C>> futures,
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    if (futures.isEmpty()) {
      return doCollectEmpty(collector);
    }

    return doCollect(futures, collector);
  }

  protected <C, T> CompletionStage<T> doCollect(
      final Collection<? extends CompletionStage<? extends C>> futures,
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    final CompletableFuture<T> target = future();

    final CollectHelper<? super C, ? extends T> done =
        new CollectHelper<>(futures.size(), collector, futures, target);

    for (final CompletionStage<? extends C> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  /**
   * Shortcut for when the list of futures is empty.
   */
  @SuppressWarnings("unchecked")
  protected <C, T> CompletionStage<T> doCollectEmpty(
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    try {
      return this.completed(collector.apply((Collection<C>) EMPTY_RESULTS));
    } catch (Exception e) {
      return failed(e);
    }
  }

  @Override
  public <C, T> CompletionStage<T> collect(
      final Collection<? extends CompletionStage<? extends C>> futures,
      final StreamCollector<? super C, ? extends T> collector
  ) {
    if (futures.isEmpty()) {
      return doStreamCollectEmpty(collector);
    }

    return doStreamCollect(futures, collector);
  }

  /**
   * Shortcut for when the list of futures is empty with {@link StreamCollector}.
   */
  protected <C, T> CompletionStage<T> doStreamCollectEmpty(
      final StreamCollector<? super C, ? extends T> collector
  ) {
    try {
      return this.completed(collector.end(0, 0, 0));
    } catch (Exception e) {
      return failed(e);
    }
  }

  protected <T, C> CompletionStage<T> doStreamCollect(
      final Collection<? extends CompletionStage<? extends C>> futures,
      final StreamCollector<? super C, ? extends T> collector
  ) {
    final CompletableFuture<T> target = future();

    final CollectStreamHelper<? super C, ? extends T> done =
        new CollectStreamHelper<>(caller, futures.size(), collector, target);

    for (final CompletionStage<? extends C> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  @Override
  public <C, T> CompletionStage<T> eventuallyCollect(
      final Collection<? extends Callable<? extends CompletionStage<? extends C>>> callables,
      final StreamCollector<? super C, ? extends T> collector, int parallelism
  ) {
    if (callables.isEmpty()) {
      return doEventuallyCollectEmpty(collector);
    }

    // Special case: the specified parallelism is sufficient to run all at once.
    if (parallelism >= callables.size()) {
      return doEventuallyCollectImmediate(callables, collector);
    }

    return doEventuallyCollect(callables, collector, parallelism);
  }

  protected <T, C> CompletionStage<T> doEventuallyCollectEmpty(
      final StreamCollector<? super C, ? extends T> collector
  ) {
    final T value;

    try {
      value = collector.end(0, 0, 0);
    } catch (Exception e) {
      return failed(e);
    }

    return completed(value);
  }

  protected <C, T> CompletionStage<T> doEventuallyCollectImmediate(
      Collection<? extends Callable<? extends CompletionStage<? extends C>>> callables,
      StreamCollector<? super C, ? extends T> collector
  ) {
    final List<CompletionStage<? extends C>> futures = new ArrayList<>(callables.size());

    for (final Callable<? extends CompletionStage<? extends C>> c : callables) {
      final CompletionStage<? extends C> future;

      try {
        future = c.call();
      } catch (Exception e) {
        futures.add(this.<C>failed(e));
        continue;
      }

      futures.add(future);
    }

    return collect(futures, collector);
  }

  protected <T, C> CompletionStage<T> doEventuallyCollect(
      final Collection<? extends Callable<? extends CompletionStage<? extends C>>> callables,
      final StreamCollector<? super C, ? extends T> collector, int parallelism
  ) {
    final ExecutorService executor = executor();
    final CompletableFuture<T> future = future();
    executor.execute(
        new DelayedCollectCoordinator<>(caller, callables, collector, future, parallelism));
    return future;
  }

  @Override
  public CompletionStage<Void> collectAndDiscard(
      Collection<? extends CompletionStage<?>> futures
  ) {
    if (futures.isEmpty()) {
      return completed();
    }

    return doCollectAndDiscard(futures);
  }

  protected CompletionStage<Void> doCollectAndDiscard(
      Collection<? extends CompletionStage<?>> futures
  ) {
    final CompletableFuture<Void> target = future();

    final CollectAndDiscardHelper done = new CollectAndDiscardHelper(futures.size(), target);

    for (final CompletionStage<?> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  @Override
  public <C> Managed<C> managed(
      Supplier<? extends CompletionStage<C>> setup,
      Function<? super C, ? extends CompletionStage<Void>> teardown
  ) {
    return ConcurrentManaged.newManaged(this, caller(), setup, teardown);
  }

  @Override
  public <C> ReloadableManaged<C> reloadableManaged(
      final Supplier<? extends CompletionStage<C>> setup,
      final Function<? super C, ? extends CompletionStage<Void>> teardown
  ) {
    return ConcurrentReloadableManaged.newReloadableManaged(this, caller(), setup, teardown);
  }

  /**
   * Build a new TinyFuture instance.
   *
   * @return A builder for the TinyFuture instance.
   */
  public static TinyFutureBuilder builder() {
    return new TinyFutureBuilder();
  }

  /**
   * Bind the given collection of futures to the target future, which if cancelled, or failed will
   * do the corresponding to their collection of futures.
   *
   * @param target The future to cancel, and fail on.
   * @param futures The futures to cancel, when {@code target} is cancelled.
   */
  protected <T> void bindSignals(
      final CompletionStage<T> target, final Collection<? extends CompletionStage<?>> futures
  ) {
    target.whenCancelled(() -> {
      for (final CompletionStage<?> f : futures) {
        f.cancel();
      }
    });
  }

  @Override
  public <T> CompletionStage<RetryResult<T>> retryUntilCompleted(
      final Callable<? extends CompletionStage<T>> callable, final RetryPolicy policy
  ) {
    return retryUntilCompleted(callable, policy, clockSource);
  }

  @Override
  public <T> CompletionStage<RetryResult<T>> retryUntilCompleted(
      final Callable<? extends CompletionStage<T>> callable, final RetryPolicy policy,
      final ClockSource clockSource
  ) {
    if (scheduler == null) {
      throw new IllegalStateException("no scheduler configured");
    }

    final CompletableFuture<T> future = future();

    final RetryPolicy.Instance policyInstance = policy.apply(clockSource);

    final long start = clockSource.now();

    final RetryCallHelper<T> helper =
        new RetryCallHelper<>(start, scheduler, callable, policyInstance, future, clockSource);

    future.whenFinished(helper::finished);

    helper.next();
    return future.thenApply(result -> new RetryResult<>(result, helper.getErrors()));
  }
}
