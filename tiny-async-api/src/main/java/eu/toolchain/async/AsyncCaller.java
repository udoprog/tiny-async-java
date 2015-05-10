package eu.toolchain.async;

/**
 * The interface that governs in which thread context a specific callback should be invoked.
 *
 * The implementation of these methods will be invoked from the calling thread that interacts with the future. It also
 * provides the framework with an ability to allow the user to handle unexpected circumstances by catching and handling
 * the case when a callback invocation throw an exception.
 *
 * See the core frameworks {@code DirectAsyncCaller} for a helper implementation to accomplish this.
 *
 * @see ConcurrentFuture
 */
public interface AsyncCaller {
    /**
     * Indicate that a Managed reference has been leaked.
     *
     * @param reference The reference that was leaked.
     * @param stack The stacktrace for where it was leaked, can be {@code null} if information is unavailable.
     */
    public <T> void leakedManagedReference(T reference, StackTraceElement[] stack);

    /**
     * @return {@code true} if this caller implementation defers task to a thread, {@code false} otherwise.
     */
    public boolean isThreaded();

    /**
     * Run resolved handle on {@code FutureDone}.
     *
     * @param handle The handle to run on.
     * @see FutureDone#resolved(T)
     */
    public <T> void resolveFutureDone(FutureDone<T> handle, T result);

    /**
     * Run failed handle on {@code FutureDone}.
     *
     * @param handle The handle to run on.
     * @see FutureDone#failed(Throwable)
     */
    public <T> void failFutureDone(FutureDone<T> handle, Throwable error);

    /**
     * Run cancelled handle on {@code FutureDone}.
     *
     * @param handle The handle to run on.
     * @see FutureDone#cancelled(Throwable)
     */
    public <T> void cancelFutureDone(FutureDone<T> handle);

    /**
     * Run finished handle on {@code FutureFinished}.
     *
     * @param finishable The handle to run on.
     * @see FutureFinished#finished()
     */
    public void runFutureFinished(FutureFinished finishable);

    /**
     * Run cancelled handle on {@code FutureCancelled}.
     *
     * @param finishable The handle to run on.
     * @see FutureFinished#finished()
     */
    public void runFutureCancelled(FutureCancelled cancelled);

    /**
     * Run resolved handle on {@code FutureResolved<T>}.
     *
     * @param resolved The handle to run on.
     * @param <T> the type of the resolved value.
     * @see FutureResolved#resolved(T)
     */
    public <T> void runFutureResolved(FutureResolved<T> resolved, T result);

    /**
     * Run failed handle on {@code FutureFailed}.
     *
     * @param failed The handle to run.
     * @param cause The error thrown.
     * @see FutureResolved#fail(Throwable)
     */
    public void runFutureFailed(FutureFailed failed, Throwable cause);

    /**
     * Run resolved handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle on.
     * @param result Result to provide to collector.
     * @see StreamCollector#resolved(T)
     */
    public <T, R> void resolveStreamCollector(StreamCollector<T, R> collector, T result);

    /**
     * Run failed handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle on.
     * @param cause Error to provide to collector.
     * @see StreamCollector#failed(Throwable)
     */
    public <T, R> void failStreamCollector(StreamCollector<T, R> collector, Throwable cause);

    /**
     * Run cancelled handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle on.
     * @see StreamCollector#cancelled()
     */
    public <T, R> void cancelStreamCollector(StreamCollector<T, R> collector);
}