package eu.toolchain.async;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * The asynchronous framework.
 *
 * This type is intended to be passed around in your application, preferably through dependency injection.
 *
 * It makes the contract between the framework and your application decoupled, which has several benefits for your
 * application's code (see README for details).
 *
 * All methods exposed are fully thread-safe.
 *
 * @author udoprog
 */
public interface AsyncFramework {
    /**
     * Retrieve the default caller.
     *
     * @return The default caller.
     */
    public AsyncCaller caller();

    /**
     * Retrieve a caller implementation that is threaded, or fail if none is available.
     *
     * @return An async caller that is threaded.
     */
    public AsyncCaller threadedCaller();

    /**
     * Build a new resolvable future.
     *
     * The future is returned in a running state, and can be resolved, failed, or cancelled. See documentation for
     * {@link AsyncFuture} for details on the various states.
     *
     * These futures are guaranteed to be thread-safe, all of their public methods can be called from any thread, at any
     * time.
     * 
     * @return A new <em>resolvable</em> future.
     * @param <T> type of the future.
     */
    public <T> ResolvableFuture<T> future();

    /**
     * Returns an already resolved void future.
     * 
     * The future is immediately resolved with a {@code null} value.
     *
     * @see #resolved(Object)
     * @return A new <em>already resolved</em> future.
     */
    public AsyncFuture<Void> resolved();

    /**
     * Build an already resolved future.
     *
     * @param value The value which the future was resolved using.
     * @param <T> type of the future.
     * @return A new resolved future.
     */
    public <T> AsyncFuture<T> resolved(T value);

    /**
     * Build an already failed future.
     *
     * @param e The Error which the future is failed using.
     * @param <T> type of the future.
     * @return A new <em>failed</em> future.
     */
    public <T> AsyncFuture<T> failed(Throwable e);

    /**
     * Build an immediately cancelled future.
     *
     * @param <T> type of the future.
     * @return A new cancelled future.
     */
    public <T> AsyncFuture<T> cancelled();

    /**
     * Transform a future of type C, to a future of type T.
     *
     * Use {@link AsyncFuture#transform(Transform)} instead of this directly.
     *
     * @param future A future of type C to transform.
     * @param transform The transforming implementation to use.
     * @param <S> source type of the future.
     * @param <T> target type the future is being transformed into.
     * @return A new future of type T.
     */
    public <S, T> AsyncFuture<T> transform(AsyncFuture<S> future, Transform<? super S, ? extends T> transform);

    /**
     * Transform a future of type C, to a future of type T using lazy transformation.
     *
     * Use {@link AsyncFuture#lazyTransform(LazyTransform)} instead of this directly.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future A future of type C to transform.
     * @param transform The transforming implementation to use.
     * @param <S> source type of the future.
     * @param <T> target type the future is being transformed into.
     * @return A new future of type T.
     */
    public <S, T> AsyncFuture<T> transform(AsyncFuture<S> future, LazyTransform<? super S, ? extends T> transform);

    /**
     * Transform a failing future into a resolved future.
     *
     * Use {@link AsyncFuture#catchFailed(Transform)} instead of this directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not fail.
     */
    public <T> AsyncFuture<T> error(AsyncFuture<T> future, Transform<Throwable, ? extends T> transform);

    /**
     * Transform a failing future into a resolved future.
     *
     * Use {@link AsyncFuture#catchFailed(Transform)} instead of this directly.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not fail.
     */
    public <T> AsyncFuture<T> error(AsyncFuture<T> future, LazyTransform<Throwable, ? extends T> transform);

    /**
     * Transform a cancelled future into a resolved future.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not cancel.
     * @see AsyncFuture#catchCancelled(Transform)
     */
    public <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, Transform<Void, ? extends T> transform);

    /**
     * Transform a cancelled future into a resolved future.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not cancel.
     * @see AsyncFuture#lazyCatchCancelled(LazyTransform)
     */
    public <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, LazyTransform<Void, ? extends T> transform);

    /**
     * Build a new future that is the result of collecting all the results in a collection.
     *
     * @param futures The collection of future to collect.
     * @param <T> type of the collected future.
     * @return A new future that is the result of collecting all results.
     */
    public <T> AsyncFuture<Collection<T>> collect(Collection<? extends AsyncFuture<? extends T>> futures);

    /**
     * Build a new future that is the result of reducing the provided collection of futures using the provided
     * collector.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A new future that is the result of reducing the collection of futures.
     */
    public <S, T> AsyncFuture<T> collect(Collection<? extends AsyncFuture<? extends S>> futures,
            Collector<? super S, ? extends T> collector);

    /**
     * Build a new future that is the result of reducing the provided collection of futures using the provided
     * collector.
     *
     * This is similar to {@link #collect(Collection, Collector)}, but uses {@link StreamCollector} which operates on
     * the stream of results as they arrive.
     *
     * This allows the implementor to reduce memory usage for certain operations since all results does not have to be
     * collected.
     *
     * If the returned future ends up in a non-resolved state, this will be forwarded to the given list of futures as
     * well.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A new future that is the result of reducing the collection of futures.
     */
    public <S, T> AsyncFuture<T> collect(Collection<? extends AsyncFuture<? extends S>> futures,
            StreamCollector<? super S, ? extends T> collector);

    /**
     * Collect the results from a collection of futures, then discard them.
     *
     * Signals like cancellations and failures will be communicated in a similar fashion to
     * {@link #collect(Collection, StreamCollector)}.
     *
     * @param futures The collection of futures to collect.
     * @param <T> type of the futures being collected and discarded.
     * @return A new future that is the result of collecting the provided futures, but discarding their results.
     */
    public <T> AsyncFuture<Void> collectAndDiscard(Collection<? extends AsyncFuture<T>> futures);

    /**
     * Collect the result from a collection of futures, that are lazily created. Futures will be created using the given
     * {@code callables}, but will only create as many pending futures to be less than or equal to the given
     * {@code parallelism} setting.
     *
     * If a single future is cancelled, or failed, all the other will be as well.
     *
     * This method is intended to be used for rate-limiting requests that could potentially be difficult to stop
     * cleanly.
     *
     * @param callables The list of constructor methods.
     * @param collector The collector to reduce the result.
     * @param parallelism The number of futures that are allowed to be constructed at the same time.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A future that will be resolved when all of the collected futures are resolved.
     */
    public <S, T> AsyncFuture<T> eventuallyCollect(
            Collection<? extends Callable<? extends AsyncFuture<? extends S>>> callables,
            StreamCollector<? super S, ? extends T> collector, int parallelism);

    /**
     * Call the given callable on the default executor and track the result using a future.
     *
     * @param callable Callable to call.
     * @param <T> type of the future.
     * @return A future tracking the result of the callable.
     * @throws IllegalStateException if no default executor service is configured.
     * @see #call(Callable, ExecutorService)
     */
    public <T> AsyncFuture<T> call(Callable<? extends T> callable);

    /**
     * Call the given callable on the provided executor and track the result using a future.
     * 
     * @param callable Callable to invoke.
     * @param executor Executor service to invoke on.
     * @param <T> type of the future.
     * @return A future tracking the result of the callable.
     * @see #call(Callable, ExecutorService, ResolvableFuture)
     */
    public <T> AsyncFuture<T> call(Callable<? extends T> callable, ExecutorService executor);

    /**
     * Call the given callable and resolve the given future with its result.
     *
     * This operation happens on the provided executor.
     *
     * @param callable The resolver to use.
     * @param executor The executor to schedule the resolver on.
     * @param future The future to resolve.
     * @param <T> type of the future.
     * @return The future that will be resolved.
     */
    public <T> AsyncFuture<T> call(Callable<? extends T> callable, ExecutorService executor, ResolvableFuture<T> future);

    /**
     * Setup a managed reference.
     *
     * @param setup The setup method for the managed reference.
     * @param <T> type of the managed reference.
     * @return The managed reference.
     */
    public <T> Managed<T> managed(ManagedSetup<T> setup);
}
