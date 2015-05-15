package eu.toolchain.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * An interface that defines a contract with a computation that could be asynchronous.
 *
 * <h4>Thread Safety</h4>
 *
 * <p>
 * All public methods exposed in {@code AsyncFuture} are fully <em>thread-safe</em>, guaranteeing that interactions with
 * the future atomic.
 * </p>
 *
 * <h4>States</h4>
 *
 * <p>
 * A future has four states.
 * </p>
 *
 * <ul>
 * <li><em>running</em>, which indicates that the future is currently active, and has not reached an end-state.</li>
 * <li><em>resolved</em>, which indicates that the computation was successful, and produced a result.</li>
 * <li><em>failed</em>, which indicates that the computation failed through an exception, which can be fetched for
 * inspection.</li>
 * <li><em>cancelled</em>, which indicates that the computation was cancelled.</li>
 * </ul>
 *
 * <p>
 * The last three states are characterized as <em>end states</em>, a future can only transition into one of these, and
 * when in an end-state will never go into another state. If a future is in and end state it is considered <em>done</em>
 * , as is indicated by the {@link #isDone()} method.
 * </p>
 *
 * @param <T> The type being provided by the future.
 *
 * @author udoprog
 */
public interface AsyncFuture<T> extends java.util.concurrent.Future<T> {
    /**
     * Check if future is resolved.
     *
     * @return {@code true} if the future is in a resolved state, otherwise {@code false}.
     * @see #isDone()
     */
    public boolean isResolved();

    /**
     * Check if future is failed.
     *
     * @return {@code true} if the future is in a failed state, otherwise {@code false}.
     * @see #isDone()
     */
    public boolean isFailed();

    /**
     * Cancel the future.
     *
     * This will not interrupt an in progress computation, but it could prevent future ones from being executed.
     *
     * @return {@code true} if the future was cancelled because of this call. {@code false} otherwise.
     */
    public boolean cancel();

    /**
     * This implementation will do nothing, unless the underlying implementation is a resolvable future.
     *
     * Failure state is a fundamental component of the computation, and should only be made available to
     * {@link ResolvableFuture}.
     *
     * @deprecated Use {@code ResolvableFuture#fail(Throwable)} instead, this method will be removed in {@literal 2.0}.
     **/
    @Deprecated
    public boolean fail(Throwable cause);

    /**
     * Get the cause of a failed future.
     *
     * @see #isFailed()
     * @throws IllegalStateException if the future is not in the failed state.
     * @return The exception that cause the future to fail.
     */
    public Throwable cause();

    /**
     * Get the result of the future.
     *
     * @throws IllegalStateException if the result is not available.
     * @throws ExecutionException if the computation threw an exception.
     */
    public T getNow() throws ExecutionException, CancellationException;

    /**
     * Register a future that will be cancelled by this future.
     *
     * @param other Other future to bind to.
     * @return This future.
     */
    public AsyncFuture<T> bind(AsyncFuture<?> other);

    /**
     * Register a listener to be called when this future finishes for any reason.
     *
     * @param finishable Function to be fired.
     * @return This future.
     */
    public AsyncFuture<T> on(FutureFinished finishable);

    /**
     * Register an listener to be called when this future is cancelled.
     *
     * @param cancelled Listener to fire.
     * @return This future.
     */
    public AsyncFuture<T> on(FutureCancelled cancelled);

    /**
     * Register a listener to be called when this future is resolved.
     *
     * @param resolved Listener to fire.
     * @return This future.
     */
    public AsyncFuture<T> on(FutureResolved<? super T> resolved);

    /**
     * Register a listener that is called on all three types of events for this future; resolved, failed, and cancelled.
     *
     * @param done Listener to fire.
     * @return This future.
     */
    public AsyncFuture<T> on(FutureDone<? super T> done);

    /**
     * Register a listener that is called when a future is failed.
     *
     * @param failed Listener to fire.
     * @return This future.
     */
    public AsyncFuture<T> on(FutureFailed failed);

    /**
     * Registers a listener to be called when this future finishes.
     *
     * The type of the listener is ignored.
     *
     * @param done Listener to fire.
     * @return This future.
     * @deprecated Use {@link #on(FutureDone)} instead after it got looser signature.
     */
    @Deprecated
    public AsyncFuture<T> onAny(FutureDone<? super T> done);

    /**
     * Transforms the value of this future into another type using a transformer function.
     *
     * <pre>
     * Future<T> (this) - *using transformer* -> Future<C>
     * </pre>
     *
     * Use this if the transformation performed does not require any more async operations.
     *
     * <pre>
     * {@code
     *   Future<Integer> first = asyncOperation();
     *
     *   Future<Double> second = future.transform(new Transformer<Integer, Double>() {
     *     Double transform(Integer result) {
     *       return result.doubleValue();
     *     }
     *   };
     *
     *   # use second
     * }
     * </pre>
     *
     * @param transform
     * @return
     */
    public <R> AsyncFuture<R> transform(Transform<? super T, ? extends R> transform);

    /**
     * Transforms the value of one future into another using a deferred transformer function.
     *
     * <pre>
     * Future<T> (this) - *using deferred transformer* -> Future<C>
     * </pre>
     *
     * A deferred transformer is expected to return a compatible future that when resolved will resolve the future that
     * this function returns.
     *
     * <pre>
     * {@code
     *   Future<Integer> first = asyncOperation();
     *
     *   Future<Double> second = first.transform(new Transformer<Integer, Double>() {
     *     void transform(Integer result, Future<Double> future) {
     *       future.finish(result.doubleValue());
     *     }
     *   };
     *
     *   # use second
     * }
     * </pre>
     *
     * @param transform The function to use when transforming the value.
     * @return A future of type <C> which resolves with the transformed value.
     */
    public <R> AsyncFuture<R> transform(LazyTransform<? super T, R> transform);

    /**
     * Transform an error into something useful.
     *
     * @param transform The transformation to use.
     */
    public AsyncFuture<T> error(Transform<Throwable, ? extends T> transform);

    /**
     * Transform an error into something useful.
     *
     * @param transform The transformation to use.
     */
    public AsyncFuture<T> error(LazyTransform<Throwable, T> transform);

    /**
     * Transform something cancelled into something useful.
     *
     * @param transform The transformation to use.
     */
    public AsyncFuture<T> cancelled(Transform<Void, ? extends T> transform);

    /**
     * Transform something cancelled into something useful using a lazy operation.
     *
     * @param transform The transformation to use.
     */
    public AsyncFuture<T> cancelled(LazyTransform<Void, T> transform);
}
