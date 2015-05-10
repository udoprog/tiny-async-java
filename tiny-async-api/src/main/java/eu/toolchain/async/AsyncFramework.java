package eu.toolchain.async;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * An Async framework implementation.
 *
 * There is only one implementation {@link TinyAsync}, but this simplifies passing it around your application and
 * testing.
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
     * These futures are guaranteed to be thread-safe, all of their public methods can be called from any thread, at any
     * time.
     *
     * @see #future(AsyncCaller)
     */
    public <T> ResolvableFuture<T> future();

    /**
     * Returns an already resolved void future.
     *
     * @see #resolved(Void, AsyncCaller)
     */
    public AsyncFuture<Void> resolved();

    /**
     * Build an already resolved future.
     *
     * @param value The value which the future was resolved using.
     * @return A new resolved future.
     */
    public <T> AsyncFuture<T> resolved(T value);

    /**
     * Build an already failed future.
     *
     * @param e The Error which the future is failed using.
     * @return A new failed future.
     */
    public <T> AsyncFuture<T> failed(Throwable e);

    /**
     * Build an immediately cancelled future.
     *
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
     * @return A new future of type T.
     */
    public <C, T> AsyncFuture<T> transform(AsyncFuture<C> future, Transform<? super C, ? extends T> transform);

    /**
     * Transform a future of type C, to a future of type T using lazy transformation.
     *
     * Use {@link AsyncFuture#transform(LazyTransform)} instead of this directly.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future A future of type C to transform.
     * @param transform The transforming implementation to use.
     * @return A new future of type T.
     */
    public <C, T> AsyncFuture<T> transform(AsyncFuture<C> future, LazyTransform<? super C, ? extends T> transform);

    /**
     * Transform a failing future into a resolved future.
     *
     * Use {@link AsyncFuture#error(Transform)} instead of this directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not fail.
     */
    public <T> AsyncFuture<T> error(AsyncFuture<T> future, Transform<Throwable, ? extends T> transform);

    /**
     * Transform a failing future into a resolved future.
     *
     * Use {@link AsyncFuture#error(Transform)} instead of this directly.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not fail.
     */
    public <T> AsyncFuture<T> error(AsyncFuture<T> future, LazyTransform<Throwable, ? extends T> transform);

    /**
     * Transform a cancelled future into a resolved future.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not cancel.
     * @see AsyncFuture#cancelled(Transform)
     */
    public <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, Transform<Void, ? extends T> transform);

    /**
     * Transform a cancelled future into a resolved future.
     *
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not cancel.
     * @see AsyncFuture#cancelled(LazyTransform)
     */
    public <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, LazyTransform<Void, ? extends T> transform);

    /**
     * Build a new future that is the result of collecting all the results in a collection.
     *
     * @param futures The collection of future to collect.
     * @return A new future that is the result of collecting all results.
     */
    public <T> AsyncFuture<Collection<T>> collect(Collection<? extends AsyncFuture<? extends T>> futures);

    /**
     * Build a new future that is the result of reducing the provided collection of futures using the provided
     * collector.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @return A new future that is the result of reducing the collection of futures.
     */
    public <C, T> AsyncFuture<T> collect(Collection<? extends AsyncFuture<? extends C>> futures,
            Collector<? super C, ? extends T> collector);

    /**
     * Build a new future that is the result of reducing the provided collection of futures using the provided
     * collector.
     *
     * This is similar to {@link #collect(List, Collector)}, but uses {@link StreamCollector} which operates on the
     * stream of results as they arrive.
     *
     * This allows the implementor to reduce memory usage for certain operations since all results does not have to be
     * collected.
     *
     * If the returned future ends up in a non-resolved state, this will be forwarded to the given list of futures as
     * well.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @return A new future that is the result of reducing the collection of futures.
     */
    public <C, T> AsyncFuture<T> collect(Collection<? extends AsyncFuture<? extends C>> futures,
            StreamCollector<? super C, ? extends T> collector);

    /**
     * Collect the results from a collection of futures, then discard them.
     *
     * Signals like cancellations and failures will be communicated in a similar fashion to
     * {@link #collect(Collection, StreamCollector, AsyncCaller)}.
     *
     * @param futures The collection of futures to collect.
     * @return A new future that is the result of collecting the provided futures, but discarding their results.
     */
    public <C> AsyncFuture<Void> collectAndDiscard(Collection<? extends AsyncFuture<C>> futures);

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
     * @return
     */
    public <C, T> AsyncFuture<T> eventuallyCollect(
            Collection<? extends Callable<? extends AsyncFuture<? extends C>>> callables,
            StreamCollector<? super C, ? extends T> collector, int parallelism);

    /**
     * Call the given callable.
     *
     * @see #call(Callable, Executor, ResolvableFuture)
     */
    public <C> AsyncFuture<C> call(Callable<? extends C> callable);

    /**
     * Call the given callable on the provided executor.
     *
     * @see #call(Callable, Executor, ResolvableFuture)
     */
    public <C> AsyncFuture<C> call(Callable<? extends C> callable, ExecutorService executor);

    /**
     * Call the given callable and resolve the given future with its result.
     *
     * This operation happens on the provided executor.
     *
     * @param callable The resolver to use.
     * @param executor The executor to schedule the resolver on.
     * @param future The future to resolve.
     * @return The future that will be resolved.
     */
    public <C> AsyncFuture<C> call(Callable<? extends C> callable, ExecutorService executor, ResolvableFuture<C> future);

    /**
     * Setup a managed reference.
     *
     * @param setup The setup method for the managed reference.
     * @return The managed reference.
     */
    public <C> Managed<C> managed(ManagedSetup<C> setup);
}
