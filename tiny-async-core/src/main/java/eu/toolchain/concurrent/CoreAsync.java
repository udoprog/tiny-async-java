package eu.toolchain.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Entry point to the tiny async framework.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 *   final Async async = CoreAsync.builder().build();
 * }</pre>
 */
@RequiredArgsConstructor
public class CoreAsync implements Async {
  private static final Collection<Object> EMPTY_RESULTS = Collections.emptyList();

  /**
   * Default executor to use when resolving asynchronously.
   */
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduler;
  /**
   * Default set of helper functions for calling callbacks.
   */
  @NonNull
  private final Caller caller;
  @NonNull
  private final ClockSource clockSource;
  @NonNull
  private final ManagedOptions managedOptions;

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

  public Caller caller() {
    return caller;
  }

  @Override
  public <C> Stage<C> call(final Callable<? extends C> callable) {
    return doCall(callable, executor(), this.completable());
  }

  @Override
  public <C> Stage<C> call(
      final Callable<? extends C> callable, final ExecutorService executor
  ) {
    return doCall(callable, executor, this.completable());
  }

  public <C> Stage<C> doCall(
      final Callable<? extends C> callable, final ExecutorService executor,
      final Completable<C> future
  ) {
    final Runnable runnable = () -> {
      // completable is already done, do not perform potentially expensive operation.
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
  public <T> Completable<T> completable() {
    return new ConcurrentCompletable<>(caller);
  }

  @Override
  public Stage<Void> completed() {
    return completed(null);
  }

  @Override
  public <T> Stage<T> completed(T value) {
    return new ImmediateCompleted<>(caller, value);
  }

  @Override
  public <T> Stage<T> failed(Throwable e) {
    return new ImmediateFailed<>(caller, e);
  }

  @Override
  public <T> Stage<T> cancelled() {
    return new ImmediateCancelled<T>(caller);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Stage<Collection<T>> collect(
      final Collection<? extends Stage<? extends T>> futures
  ) {
    if (futures.isEmpty()) {
      return completed((Collection<T>) EMPTY_RESULTS);
    }

    return collect(futures, Function.identity());
  }

  @Override
  public <C, T> Stage<T> collect(
      final Collection<? extends Stage<? extends C>> futures,
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    if (futures.isEmpty()) {
      return doCollectEmpty(collector);
    }

    return doCollect(futures, collector);
  }

  protected <C, T> Stage<T> doCollect(
      final Collection<? extends Stage<? extends C>> futures,
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    final Completable<T> target = completable();

    final CollectHelper<? super C, ? extends T> done =
        new CollectHelper<>(futures.size(), collector, futures, target);

    for (final Stage<? extends C> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  /**
   * Shortcut for when the list of futures is empty.
   *
   * @param collector collector to apply
   */
  @SuppressWarnings("unchecked")
  <C, T> Stage<T> doCollectEmpty(
      final Function<? super Collection<C>, ? extends T> collector
  ) {
    try {
      return this.completed(collector.apply((Collection<C>) EMPTY_RESULTS));
    } catch (Exception e) {
      return failed(e);
    }
  }

  @Override
  public <T, U> Stage<U> streamCollect(
      final Collection<? extends Stage<? extends T>> futures,
      final StreamCollector<? super T, ? extends U> collector
  ) {
    if (futures.isEmpty()) {
      return doStreamCollectEmpty(collector);
    }

    return doStreamCollect(futures, collector);
  }

  /**
   * Shortcut for when the list of futures is empty with {@link StreamCollector}.
   *
   * @param collector collector to apply
   * @param <T> source type
   * @param <U> target type
   */
  <T, U> Stage<U> doStreamCollectEmpty(
      final StreamCollector<? super T, ? extends U> collector
  ) {
    try {
      return this.completed(collector.end(0, 0, 0));
    } catch (Exception e) {
      return failed(e);
    }
  }

  /**
   * Perform collection for {@link StreamCollector}.
   *
   * @param futures futures to apply to collector
   * @param collector collector to apply
   * @param <T> source type
   * @param <U> target type
   */
  <T, U> Stage<U> doStreamCollect(
      final Collection<? extends Stage<? extends T>> futures,
      final StreamCollector<? super T, ? extends U> collector
  ) {
    final Completable<U> target = completable();

    final StreamCollectHelper<? super T, ? extends U> done =
        new StreamCollectHelper<>(caller, futures.size(), collector, target);

    for (final Stage<? extends T> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  @Override
  public <T, U> Stage<U> endCollect(
      final Collection<? extends Stage<? extends T>> futures,
      final EndCollector<? extends U> collector
  ) {
    if (futures.isEmpty()) {
      return doEndCollectEmpty(collector);
    }

    return doEndCollect(futures, collector);
  }

  <T> Stage<T> doEndCollectEmpty(
      final EndCollector<? extends T> collector
  ) {
    try {
      return this.completed(collector.apply(0, 0, 0));
    } catch (Exception e) {
      return failed(e);
    }
  }

  <T, U> Stage<U> doEndCollect(
      final Collection<? extends Stage<? extends T>> futures,
      final EndCollector<? extends U> collector
  ) {
    final Completable<U> target = completable();

    final EndCollectHelper<? extends U> done =
        new EndCollectHelper<>(futures.size(), collector, target);

    for (final Stage<? extends T> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  @Override
  public <C, T> Stage<T> eventuallyCollect(
      final Collection<? extends Callable<? extends Stage<? extends C>>> callables,
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

  <T, C> Stage<T> doEventuallyCollectEmpty(
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

  <C, T> Stage<T> doEventuallyCollectImmediate(
      Collection<? extends Callable<? extends Stage<? extends C>>> callables,
      StreamCollector<? super C, ? extends T> collector
  ) {
    final List<Stage<? extends C>> futures = new ArrayList<>(callables.size());

    for (final Callable<? extends Stage<? extends C>> c : callables) {
      final Stage<? extends C> future;

      try {
        future = c.call();
      } catch (Exception e) {
        futures.add(this.<C>failed(e));
        continue;
      }

      futures.add(future);
    }

    return streamCollect(futures, collector);
  }

  /**
   * Perform an eventual collection.
   *
   * @param tasks tasks to invoke for futures
   * @param collector collector to apply
   * @param parallelism number of tasks to run in parallel
   * @param <T> source type
   * @param <U> target type
   * @return a completable
   */
  <T, U> Stage<U> doEventuallyCollect(
      final Collection<? extends Callable<? extends Stage<? extends T>>> tasks,
      final StreamCollector<? super T, ? extends U> collector, int parallelism
  ) {
    final ExecutorService executor = executor();
    final Completable<U> future = completable();
    executor.execute(
        new DelayedCollectCoordinator<>(caller, tasks, collector, future, parallelism));
    return future;
  }

  @Override
  public Stage<Void> collectAndDiscard(
      Collection<? extends Stage<?>> futures
  ) {
    if (futures.isEmpty()) {
      return completed();
    }

    return doCollectAndDiscard(futures);
  }

  /**
   * Perform a collect and discard.
   *
   * @param futures futures to discard
   * @return a completable
   */
  Stage<Void> doCollectAndDiscard(
      Collection<? extends Stage<?>> futures
  ) {
    final Completable<Void> target = completable();

    final CollectAndDiscardHelper done = new CollectAndDiscardHelper(futures.size(), target);

    for (final Stage<?> q : futures) {
      q.handle(done);
    }

    bindSignals(target, futures);
    return target;
  }

  @Override
  public <C> Managed<C> managed(
      Supplier<? extends Stage<C>> setup, Function<? super C, ? extends Stage<Void>> teardown
  ) {
    return ConcurrentManaged.newManaged(this, caller(), managedOptions, setup, teardown);
  }

  @Override
  public <C> ReloadableManaged<C> reloadableManaged(
      final Supplier<? extends Stage<C>> setup,
      final Function<? super C, ? extends Stage<Void>> teardown
  ) {
    return new ConcurrentReloadableManaged<>(this, caller(), managedOptions, setup, teardown);
  }

  /**
   * Bind the given collection of futures to the target completable, which if cancelled, or failed
   * will do the corresponding to their collection of futures.
   *
   * @param target The completable to cancel, and fail on.
   * @param futures The futures to cancel, when {@code target} is cancelled.
   */
  void bindSignals(
      final Stage<?> target, final Collection<? extends Stage<?>> futures
  ) {
    target.whenCancelled(() -> {
      for (final Stage<?> f : futures) {
        f.cancel();
      }
    });
  }

  @Override
  public <T> Stage<RetryResult<T>> retryUntilCompleted(
      final Callable<? extends Stage<T>> callable, final RetryPolicy policy
  ) {
    return retryUntilCompleted(callable, policy, clockSource);
  }

  @Override
  public <T> Stage<RetryResult<T>> retryUntilCompleted(
      final Callable<? extends Stage<T>> callable, final RetryPolicy policy,
      final ClockSource clockSource
  ) {
    if (scheduler == null) {
      throw new IllegalStateException("no scheduler configured");
    }

    final Completable<T> future = completable();

    final Supplier<RetryDecision> policyInstance = policy.newInstance(clockSource);

    final long start = clockSource.now();

    final RetryCallHelper<T> helper =
        new RetryCallHelper<>(start, scheduler, callable, policyInstance, future, clockSource);

    future.whenFinished(helper::finished);

    helper.next();
    return future.thenApply(result -> new RetryResult<>(result, helper.getErrors()));
  }

  static String formatStack(final Stream<StackTraceElement> stack, final String prefix) {
    final StringJoiner joiner = new StringJoiner("\n" + prefix, prefix, "");
    stack.map(Object::toString).forEach(joiner::add);
    return joiner.toString();
  }

  static Throwable buildCollectedException(Collection<Throwable> errors) {
    final Iterator<Throwable> it = errors.iterator();
    final Throwable first = it.next();

    while (it.hasNext()) {
      first.addSuppressed(it.next());
    }

    return first;
  }

  /**
   * Build a new CoreAsync instance.
   *
   * @return a builder for the CoreAsync instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Caller caller;
    private boolean threaded;
    private boolean recursionSafe;
    private long maxRecursionDepth = 100;
    private ExecutorService executor;
    private ExecutorService callerExecutor;
    private ScheduledExecutorService scheduler;
    private ClockSource clockSource = ClockSource.system();
    private ManagedOptions managedOptions = ManagedOptions.builder().build();

    Builder() {
    }

    /**
     * Configure that all caller invocation, and async tasks should be using a thread pool.
     *
     * <p>This will cause the configuration of TinyTask to throw an exception if an executor service
     * is not available for all purposes.
     *
     * @param threaded {@code true} if all tasks should be executed on a thread pool
     * @return this builder
     */
    public Builder threaded(final boolean threaded) {
      this.threaded = threaded;
      return this;
    }

    /**
     * Configure that all caller invocations should use a recursion safe mechanism. In the normal
     * case this doesn't change the behaviour of caller and threadedCaller, but when deep recursion
     * is detected in the current thread the next recursive doCall is deferred to a separate thread.
     *
     * <p>Recursion is tracked for all threads that doCall the AsyncCallers.
     *
     * <p>This will make even the non-threaded caller use a thread in the case of deep recursion.
     *
     * @param recursionSafe {@code true} if all caller invocations should be done with a recursion
     * safe mechanism.
     * @return this builder
     */
    public Builder recursionSafe(final boolean recursionSafe) {
      this.recursionSafe = recursionSafe;
      return this;
    }

    /**
     * Configure how many recursions should be allowed.
     *
     * <p>This implies enabling {@link #recursionSafe}.
     *
     * @param maxRecursionDepth The max number of times that a caller may go through {@link Caller}
     * in a single thread.
     * @return this builder
     */
    public Builder maxRecursionDepth(final long maxRecursionDepth) {
      this.maxRecursionDepth = maxRecursionDepth;
      this.recursionSafe = true;
      return this;
    }

    /**
     * Specify an asynchronous caller implementation.
     *
     * <p>The 'caller' defines how handles are invoked. The simplest implementations are based of
     * {@code DirectCaller} , which causes the doCall to be performed directly in the calling
     * thread.
     *
     * @param caller caller to configure
     * @return this builder
     */
    public Builder caller(final Caller caller) {
      Objects.requireNonNull(caller, "caller");

      this.caller = caller;
      return this;
    }

    /**
     * Configure the default executor to use for caller invocation,and asynchronous tasks submitted
     * through {@link Async#call(Callable)}.
     *
     * @param executor Executor to use
     * @return this builder
     */
    public Builder executor(final ExecutorService executor) {
      Objects.requireNonNull(executor, "executor");

      this.executor = executor;
      return this;
    }

    /**
     * Specify a separate executor to use for caller (internal handle) invocation.
     *
     * <p>Implies use of threaded caller.
     *
     * @param callerExecutor Executor to use for callers
     * @return this builder
     */
    public Builder callerExecutor(final ExecutorService callerExecutor) {
      Objects.requireNonNull(callerExecutor, "callerExecutor");

      this.threaded = true;
      this.callerExecutor = callerExecutor;
      return this;
    }

    /**
     * Specify a scheduler to use with the built CoreAsync instance.
     *
     * @param scheduler The scheduler to use
     * @return this builder
     */
    public Builder scheduler(final ScheduledExecutorService scheduler) {
      this.scheduler = scheduler;
      return this;
    }

    /**
     * Configure clock source.
     *
     * <p>A clock source is used to determine what the current time is in order to do timing-related
     * tasks like retrying an action until it has been completed with a back-off.
     *
     * @param clockSource clock source to configure
     * @return this builder
     * @see Async#retryUntilCompleted(java.util.concurrent.Callable, RetryPolicy)
     */
    public Builder clockSource(final ClockSource clockSource) {
      Objects.requireNonNull(clockSource, "clockSource");

      this.clockSource = clockSource;
      return this;
    }

    public Builder managedOptions(final ManagedOptions managedOptions) {
      Objects.requireNonNull(managedOptions, "managedOptions");

      this.managedOptions = managedOptions;
      return this;
    }

    public CoreAsync build() {
      final ExecutorService defaultExecutor = this.executor;
      final ExecutorService callerExecutor = setupCallerExecutor(defaultExecutor);
      final Caller caller = setupCaller(callerExecutor);

      return new CoreAsync(defaultExecutor, scheduler, caller, clockSource, managedOptions);
    }

    /**
     * Attempt to setup a caller executor according to the provided implementation.
     *
     * @param defaultExecutor configured default executor
     * @return caller executor
     */
    private ExecutorService setupCallerExecutor(final ExecutorService defaultExecutor) {
      if (callerExecutor != null) {
        return callerExecutor;
      }

      if (defaultExecutor != null) {
        return defaultExecutor;
      }

      return null;
    }

    /**
     * Setup the completable caller.
     *
     * @param callerExecutor configured caller executor
     * @return A caller implementation according to the provided configuration.
     */
    private Caller setupCaller(final ExecutorService callerExecutor) {
      Caller caller;

      if (this.caller != null) {
        caller = this.caller;
      } else {
        caller = new PrintStreamCaller(System.err);
      }

      if (threaded) {
        if (callerExecutor == null) {
          throw new IllegalStateException("#threaded enabled, but no caller executor configured");
        }

        caller = new ExecutorCaller(callerExecutor, caller);
      }

      if (recursionSafe) {
        if (callerExecutor == null) {
          throw new IllegalStateException(
              "#recursionSafe enabled, but no caller executor configured");
        }

        caller = new RecursionSafeCaller(callerExecutor, caller, maxRecursionDepth);
      }

      return caller;
    }
  }
}
