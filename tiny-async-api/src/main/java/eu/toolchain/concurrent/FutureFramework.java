package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entry-point of the future framework.
 *
 * <p>This type is intended to be passed around in your application, preferably through dependency
 * injection.
 *
 * <p>It makes the contract between the framework and your application decoupled, which has several
 * benefits for your application's code (see README for details).
 *
 * @author udoprog
 */
public interface FutureFramework {
  /**
   * Build a new resolvable future.
   * <p>
   * The future is returned in a running state, and can be completed, failed, or cancelled. See
   * documentation for {@link CompletionStage} for details on the various states.
   * <p>
   * These futures are guaranteed to be thread-safe, all of their methods can be called from any
   * thread, at any time.
   *
   * @param <T> type of the future.
   * @return A new <em>resolvable</em> future.
   */
  <T> CompletableFuture<T> future();

  /**
   * Returns an already completed void future.
   * <p>
   * The future is immediately completed with a {@code null} value.
   *
   * @return A new <em>already completed</em> future.
   * @see #completed(Object)
   */
  CompletionStage<Void> completed();

  /**
   * Build an already completed future.
   *
   * @param value The value which the future was completed using.
   * @param <T> type of the future.
   * @return A new completed future.
   */
  <T> CompletionStage<T> completed(T value);

  /**
   * Build an already failed future.
   *
   * @param e The Error which the future is failed using.
   * @param <T> type of the future.
   * @return A new <em>failed</em> future.
   */
  <T> CompletionStage<T> failed(Throwable e);

  /**
   * Build an immediately cancelled future.
   *
   * @param <T> type of the future.
   * @return A new cancelled future.
   */
  <T> CompletionStage<T> cancelled();

  /**
   * Collect the result of multiple futures from a stream.
   *
   * @param stream stream to collect futures from
   * @param <T> type of the collected futures
   * @return a new future
   */
  default <T> CompletionStage<Collection<T>> collect(
      final Stream<? extends CompletionStage<T>> stream
  ) {
    return collect(stream.collect(Collectors.toList()));
  }

  /**
   * Build a new future that is the result of collecting all the results in a collection.
   *
   * @param futures the collection of futures
   * @param <T> type of the collected future
   * @return a new future
   */
  <T> CompletionStage<Collection<T>> collect(
      Collection<? extends CompletionStage<? extends T>> futures
  );

  /**
   * Build a new future that is the result of reducing the provided collection of futures using
   * the provided collector.
   *
   * @param futures the collection of futures
   * @param collector the collector
   * @param <S> source type of the collected futures.
   * @param <T> target type the collected futures are being transformed into
   * @return a new future
   */
  <S, T> CompletionStage<T> collect(
      Collection<? extends CompletionStage<? extends S>> futures,
      Function<? super Collection<S>, ? extends T> collector
  );

  /**
   * Build a new future that is the result of reducing the provided collection of futures using
   * the provided collector.
   * <p>
   * This is similar to {@link #collect(Collection, Function)}, but uses {@link StreamCollector}
   * which operates on the stream of results as they arrive.
   * <p>
   * This allows the implementor to reduce memory usage for certain operations since all results
   * does not have to be collected.
   * <p>
   * If the returned future ends up in a non-completed state, this will be forwarded to the given
   * list of futures as well.
   *
   * @param futures the collection of futures
   * @param collector the collector
   * @param <S> source type of the collected futures.
   * @param <T> target type the collected futures are being transformed into
   * @return a new future
   */
  <S, T> CompletionStage<T> collect(
      Collection<? extends CompletionStage<? extends S>> futures,
      StreamCollector<? super S, ? extends T> collector
  );

  /**
   * Collect the results from a stream of futures, then discard them.
   *
   * @param stream stream to collect results from
   * @return a new future
   */
  default CompletionStage<Void> collectAndDiscard(
      final Stream<? extends CompletionStage<?>> stream
  ) {
    return collectAndDiscard(stream.collect(Collectors.toList()));
  }

  /**
   * Collect the results from a collection of futures, then discard them.
   * <p>
   * Signals like cancellations and failures will be communicated in a similar fashion to {@link
   * #collect(Collection, StreamCollector)}.
   *
   * @param futures collection to collect
   * @return a new future
   */
  CompletionStage<Void> collectAndDiscard(Collection<? extends CompletionStage<?>> futures);

  /**
   * Collect the result from a collection of operations that are lazily created.
   * <p>
   * Futures will be created using the given {@code callables}, but will only create as many pending
   * futures to be less than or equal to the given {@code parallelism} setting.
   * <p>
   * If a single future is cancelled, or failed, all the other will be as well.
   * <p>
   * This method is intended to be used for rate-limiting requests that could potentially be
   * difficult to stop cleanly.
   *
   * @param callables the collection of operations
   * @param collector the collector
   * @param parallelism number of futures that are allowed to be pending at the same time
   * @param <S> source type of the collected futures
   * @param <T> target type the collected futures are being transformed into
   * @return a new future that is completed when all operations are completed
   */
  <S, T> CompletionStage<T> eventuallyCollect(
      Collection<? extends Callable<? extends CompletionStage<? extends S>>> callables,
      StreamCollector<? super S, ? extends T> collector, int parallelism
  );

  /**
   * Call the given callable on the default executor and track the result using a future.
   *
   * @param callable operation to call
   * @param <T> type of the operation
   * @return a new future
   * @throws IllegalStateException if no default executor service is configured
   * @see #call(Callable, ExecutorService)
   */
  <T> CompletionStage<T> call(Callable<? extends T> callable);

  /**
   * Call the given callable on the provided executor and track the result using a future.
   *
   * @param callable operation to call
   * @param executor executor service to invoke on
   * @param <T> type of the operation
   * @return a new future
   */
  <T> CompletionStage<T> call(Callable<? extends T> callable, ExecutorService executor);

  /**
   * Setup a managed reference.
   *
   * @param setup setup method for the managed reference
   * @param teardown teardown method for the manager reference
   * @param <T> type of the managed reference
   * @return a managed reference
   */
  <T> Managed<T> managed(
      Supplier<? extends CompletionStage<T>> setup,
      Function<? super T, ? extends CompletionStage<Void>> teardown
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
      Supplier<? extends CompletionStage<T>> setup,
      Function<? super T, ? extends CompletionStage<Void>> teardown
  );

  /**
   * Retry the given action until it has been completed, or the provided {@link
   * eu.toolchain.concurrent.RetryPolicy} expire.
   *
   * @param callable action to run
   * @param policy retry policy to use
   * @param <T> the type returned by the action
   * @return a future tied to the operation
   * @see #retryUntilCompleted(java.util.concurrent.Callable, RetryPolicy, ClockSource)
   */
  <T> CompletionStage<RetryResult<T>> retryUntilCompleted(
      Callable<? extends CompletionStage<T>> callable, RetryPolicy policy
  );

  /**
   * Retry the given action until it has been completed, or the provided {@link
   * eu.toolchain.concurrent.RetryPolicy} expire.
   *
   * @param callable action to run
   * @param policy retry policy to use
   * @param clockSource clock source to use
   * @param <T> the type returned by the action
   * @return a future tied to the operation
   */
  <T> CompletionStage<RetryResult<T>> retryUntilCompleted(
      Callable<? extends CompletionStage<T>> callable, RetryPolicy policy, ClockSource clockSource
  );
}
