package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entry-point of the async framework.
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
   * <p>These futures are guaranteed to be thread-safe, all of their methods can be called from any
   * thread, at any time.
   *
   * @param <T> type of the completable.
   * @return A new <em>resolvable</em> completable.
   */
  <T> Completable<T> completable();

  /**
   * Returns an already completed void completable.
   *
   * <p>The completable is immediately completed with a {@code null} value.
   *
   * @return A new <em>already completed</em> completable.
   * @see #completed(Object)
   */
  Stage<Void> completed();

  /**
   * Build an already completed completable.
   *
   * @param value The value which the completable was completed using.
   * @param <T> type of the completable.
   * @return A new completed completable.
   */
  <T> Stage<T> completed(T value);

  /**
   * Build an already failed completable.
   *
   * @param e The Error which the completable is failed using.
   * @param <T> type of the completable.
   * @return A new <em>failed</em> completable.
   */
  <T> Stage<T> failed(Throwable e);

  /**
   * Build an immediately cancelled completable.
   *
   * @param <T> type of the completable.
   * @return A new cancelled completable.
   */
  <T> Stage<T> cancelled();

  /**
   * Collect the result of multiple futures from a stream.
   *
   * @param stream stream to collect futures from
   * @param <T> type of the collected futures
   * @return a new completable
   */
  default <T> Stage<Collection<T>> collect(
      final Stream<? extends Stage<T>> stream
  ) {
    return collect(stream.collect(Collectors.toList()));
  }

  /**
   * Build a new completable that is the result of collecting all the results in a collection.
   *
   * @param futures the collection of futures
   * @param <T> type of the collected completable
   * @return a stage completed with the collection of values
   */
  <T> Stage<Collection<T>> collect(
      Collection<? extends Stage<? extends T>> futures
  );

  /**
   * Build a new completable that is the result of reducing the provided collection of futures using
   * the provided collector.
   *
   * @param futures the collection of futures
   * @param collector the collector
   * @param <T> source type of the collected futures.
   * @param <U> target type the collected futures are being transformed into
   * @return a stage bound to the collected value of the collector
   */
  <T, U> Stage<U> collect(
      Collection<? extends Stage<? extends T>> futures,
      Function<? super Collection<T>, ? extends U> collector
  );

  /**
   * Build a new stage that is the result of applying a computation on a collection of futures.
   *
   * <p>This is similar to {@link #collect(Collection, Function)}, but uses {@link StreamCollector}
   * which operates on the stream of results as they arrive.
   *
   * <p>This allows the implementor to reduce memory usage for certain operations since all results
   * does not have to be collected.
   *
   * <p>If the returned stage ends up in a non-completed state, this will be forwarded to the
   * given list of futures as well.
   *
   * @param futures the collection of futures
   * @param collector the collector
   * @param <T> source type of the collected futures.
   * @param <U> target type of the collector
   * @return a stage bound to the collected value of the collector
   */
  <T, U> Stage<U> streamCollect(
      Collection<? extends Stage<? extends T>> futures,
      StreamCollector<? super T, ? extends U> collector
  );

  /**
   * Build a new stage that is the result of applying a computation on a collection of futures.
   *
   * <p>This version only care about the number of results.
   *
   * @param futures the collection of futures
   * @param collector the collector
   * @param <T> source type of the collected futures
   * @param <U> target type of the collector
   * @return a stage bound to the collected value of the collector
   */
  <T, U> Stage<U> endCollect(
      Collection<? extends Stage<? extends T>> futures,
      EndCollector<? extends U> collector
  );

  /**
   * Collect the results from a stream of futures, then discard them.
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
   * Collect the results from a collection of futures, then discard them.
   *
   * <p>Signals like cancellations and failures will be communicated in a similar fashion to {@link
   * #streamCollect(Collection, StreamCollector)}.
   *
   * @param futures collection to collect
   * @return a new completable
   */
  Stage<Void> collectAndDiscard(Collection<? extends Stage<?>> futures);

  /**
   * Collect the result from a collection of operations that are lazily created.
   *
   * <p>Async will be created using the given {@code callables}, but will only create as many
   * pending futures to be less than or equal to the given {@code parallelism} setting.
   *
   * <p>If a single completable is cancelled, or failed, all the other will be as well.
   *
   * <p>This method is intended to be used for rate-limiting requests that could potentially be
   * difficult to stop cleanly.
   *
   * @param callables the collection of operations
   * @param collector the collector
   * @param parallelism number of futures that are allowed to be pending at the same time
   * @param <T> source type of the collected futures
   * @param <U> target type the collected futures are being transformed into
   * @return a new completable that is completed when all operations are completed
   */
  <T, U> Stage<U> eventuallyCollect(
      Collection<? extends Callable<? extends Stage<? extends T>>> callables,
      StreamCollector<? super T, ? extends U> collector, int parallelism
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
      Supplier<? extends Stage<T>> setup,
      Function<? super T, ? extends Stage<Void>> teardown
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
      Supplier<? extends Stage<T>> setup,
      Function<? super T, ? extends Stage<Void>> teardown
  );

  /**
   * Retry the given action until it has been completed, or the provided {@link
   * eu.toolchain.concurrent.RetryPolicy} expire.
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
