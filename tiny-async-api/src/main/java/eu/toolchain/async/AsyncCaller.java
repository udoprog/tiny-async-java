package eu.toolchain.async;


/**
 * The interface that governs in which thread context a specific callback should be invoked.
 *
 * The implementation of these methods will be invoked from the calling thread that interacts with the future.
 *
 * @see ConcurrentFuture
 */
public interface AsyncCaller {
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