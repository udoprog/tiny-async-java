package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The async framework.
 *
 * <p>This type is intended to be passed around in your application, preferably through dependency
 * injection.
 *
 * <p>It makes the contract between the framework and your application decoupled, which has several
 * benefits for your application's code (see README for details).
 */
public interface Async {
  /**
   * Build a new resolvable completable.
   *
   * <p>The completable is returned in a running state, and can be completed, failed, or cancelled.
   * See documentation for {@link Stage} for details on the various states.
   *
   * @param <T> type of the completable.
   * @return a new completable
   */
  <T> Completable<T> completable();

  /**
   * Returns an already completed void completable.
   *
   * <p>The completable is immediately completed with a {@code null} value.
   *
   * @return a new <em>already completed</em> completable
   * @see #completed(Object)
   */
  Stage<Void> completed();

  /**
   * Build an already completed completable.
   *
   * @param value value which the completable was completed using
   * @param <T> type of the completable
   * @return a <em>completed</em> stage
   */
  <T> Stage<T> completed(T value);

  /**
   * Build an already failed completable.
   *
   * @param e The Error which the completable is failed using.
   * @param <T> type of the completable.
   * @return a <em>failed</em> stage
   */
  <T> Stage<T> failed(Throwable e);

  /**
   * Build an immediately cancelled completable.
   *
   * @param <T> type of the completable
   * @return a cancelled stage
   */
  <T> Stage<T> cancelled();

  /**
   * Collect the result of multiple stages from a stream.
   *
   * @param stream stream to collect stages from
   * @param <T> type of the collected stages
   * @return a new stage
   */
  default <T> Stage<Collection<T>> collect(
      final Stream<? extends Stage<T>> stream
  ) {
    return collect(stream.collect(Collectors.toList()));
  }

  /**
   * Build a new completable that is the result of collecting all the results in a collection.
   *
   * @param stages the collection of stages
   * @param <T> type of the collected completable
   * @return a stage completed with the collection of values
   */
  <T> Stage<Collection<T>> collect(
      Collection<? extends Stage<? extends T>> stages
  );

  /**
   * Build a new completable that is the result of reducing the provided collection of stages using
   * the provided collector.
   *
   * @param stages the collection of stages
   * @param collector the collector
   * @param <T> source type of the collected stages
   * @param <U> target type the collected stages are being transformed into
   * @return a stage bound to the collected value of the collector
   */
  <T, U> Stage<U> collect(
      Collection<? extends Stage<? extends T>> stages,
      Function<? super Collection<T>, ? extends U> collector
  );

  /**
   * Build a new stage that is the result of applying a computation on a collection of stages.
   *
   * <p>This is similar to {@link #collect(Collection, Function)}, but uses abstractions which
   * operates on the stream of results as they arrive.
   *
   * <p>This allows the implementor to reduce memory usage for certain operations since all results
   * does not have to be collected.
   *
   * <p>If the returned stage ends up in a non-completed state, this will be forwarded to the
   * given list of stages as well.
   *
   * @param stages the collection of stages
   * @param consumer value consumer
   * @param supplier result supplier
   * @param <T> source type of the collected stages
   * @param <U> target type of the collector
   * @return a stage bound to the collected value of the collector
   */
  <T, U> Stage<U> streamCollect(
    Collection<? extends Stage<? extends T>> stages,
    Consumer<? super T> consumer, Supplier<? extends U> supplier
  );

  /**
   * Collect the results from a stream of stages, then discard them.
   *
   * @param stream stream to collect results from
   * @return a new completable
   */
  default Stage<Void> collectAndDiscard(
      final Stream<? extends Stage<?>> stream
  ) {
    return collectAndDiscard(stream.collect(Collectors.toList()));
  }

  /**
   * Collect the results from a collection of stages, then discard them.
   *
   * <p>Signals like cancellations and failures will be communicated in a similar fashion to {@link
   * #streamCollect(Collection, Consumer, Supplier)}.
   *
   * @param stages collection to collect
   * @return a new completable
   */
  Stage<Void> collectAndDiscard(Collection<? extends Stage<?>> stages);

  /**
   * Collect the result from a collection of operations that are lazily created.
   *
   * <p>Async will be created using the given {@code callables}, but will only create as many
   * pending stages to be less than or equal to the given {@code parallelism} setting.
   *
   * <p>If a single completable is cancelled, or failed, all the other will be as well.
   *
   * <p>This method is intended to be used for rate-limiting requests that could potentially be
   * difficult to stop cleanly.
   *
   * @param callables the collection of operations
   * @param consumer value consumer
   * @param supplier result supplier
   * @param parallelism number of stages that are allowed to be pending at the same time
   * @param <T> source type of the collected stages
   * @param <U> target type the collected stages are being transformed into
   * @return a new completable that is completed when all operations are completed
   */
  <T, U> Stage<U> eventuallyCollect(
    Collection<? extends Callable<? extends Stage<? extends T>>> callables,
    Consumer<? super T> consumer, Supplier<? extends U> supplier, int parallelism
  );

  /**
   * Call the given callable on the default executor and track the result using a completable.
   *
   * @param callable operation to call
   * @param <T> type of the operation
   * @return a new completable
   * @throws IllegalStateException if no default executor service is configured
   * @see #call(Callable, ExecutorService)
   */
  <T> Stage<T> call(Callable<? extends T> callable);

  /**
   * Call the given callable on the provided executor and track the result using a completable.
   *
   * @param callable operation to call
   * @param executor executor service to invoke on
   * @param <T> type of the operation
   * @return a new completable
   */
  <T> Stage<T> call(Callable<? extends T> callable, ExecutorService executor);

  /**
   * Setup a managed reference.
   *
   * @param setup setup method for the managed reference
   * @param teardown teardown method for the manager reference
   * @param <T> type of the managed reference
   * @return a managed reference
   */
  <T> Managed<T> managed(
      Supplier<? extends Stage<T>> setup, Function<? super T, ? extends Stage<Void>> teardown
  );

  /**
   * Setup a reloadable, managed reference.
   *
   * @param setup the setup method for the managed reference
   * @param teardown teardown method for the manager reference
   * @param <T> type of the managed reference
   * @return a managed reference
   */
  <T> ReloadableManaged<T> reloadableManaged(
      Supplier<? extends Stage<T>> setup, Function<? super T, ? extends Stage<Void>> teardown
  );

  /**
   * Retry the given action until it has been completed, or the provided {@link RetryPolicy} expire.
   *
   * @param callable action to run
   * @param policy retry policy to use
   * @param <T> the type returned by the action
   * @return a completable tied to the operation
   * @see #retryUntilCompleted(java.util.concurrent.Callable, RetryPolicy, ClockSource)
   */
  <T> Stage<RetryResult<T>> retryUntilCompleted(
      Callable<? extends Stage<T>> callable, RetryPolicy policy
  );

  /**
   * Retry the given action until it has been completed, or the provided {@link RetryPolicy} expire.
   *
   * @param callable action to run
   * @param policy retry policy to use
   * @param clockSource clock source to use
   * @param <T> the type returned by the action
   * @return a completable tied to the operation
   */
  <T> Stage<RetryResult<T>> retryUntilCompleted(
      Callable<? extends Stage<T>> callable, RetryPolicy policy, ClockSource clockSource
  );
}
