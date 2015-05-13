package eu.toolchain.async;

/**
 * User-defined functions to handle unexpected circumstances.
 *
 * The implementation of these methods will be invoked from the calling thread that interacts with the future.
 *
 * None of the below methods throw checked exceptions, and they are intended to never throw anything, with the exception
 * of {@code Error}. This means that the implementor is required to make sure this doesn't happen, the best way to
 * accomplish this is to wrap each callback in a try-catch statement like below.
 *
 * <pre>
 * {@code
 * new AsyncCaller() {
 *   public <T> void resolve(FutureDone<T> handle, T result) {
 *     try {
 *       handle.resolved(result);
 *     } catch(Exception e) {
 *       // log unexpected error
 *     }
 *   }
 *
 *   // .. other methods
 * }
 * }
 * </pre>
 *
 * The core of the framework provides some base classes for easily accomplishing this, most notable is
 * {@code DirectAsyncCaller}.
 *
 * @author udoprog
 */
public interface AsyncCaller {
    /**
     * Indicate that a Managed reference has been leaked.
     *
     * @param reference The reference that was leaked.
     * @param stack The stacktrace for where it was leaked, can be {@code null} if information is unavailable.
     */
    public <T> void referenceLeaked(T reference, StackTraceElement[] stack);

    /**
     * Returns {@code true} if this caller defers invocations to a separate thread.
     *
     * @return {@code true} if this caller is threaded.
     */
    public boolean isThreaded();

    /**
     * Run resolved handle on {@code FutureDone}.
     *
     * @param handle The handle to run.
     * @see FutureDone#resolved(T)
     */
    public <T> void resolve(FutureDone<T> handle, T result);

    /**
     * Run failed handle on {@code FutureDone}.
     *
     * @param handle The handle to run.
     * @see FutureDone#failed(Throwable)
     */
    public <T> void fail(FutureDone<T> handle, Throwable error);

    /**
     * Run cancelled handle on {@code FutureDone}.
     *
     * @param handle The handle to run on.
     * @see FutureDone#cancelled(Throwable)
     */
    public <T> void cancel(FutureDone<T> handle);

    /**
     * Run finished handle on {@code FutureFinished}.
     *
     * @param finishable The handle to run.
     * @see FutureFinished#finished()
     */
    public void finish(FutureFinished finishable);

    /**
     * Run cancelled handle on {@code FutureCancelled}.
     *
     * @param finishable The handle to run on.
     * @see FutureFinished#finished()
     */
    public void cancel(FutureCancelled cancelled);

    /**
     * Run resolved handle on {@code FutureResolved<T>}.
     *
     * @param resolved The handle to run.
     * @param <T> the type of the resolved value.
     * @see FutureResolved#resolved(T)
     */
    public <T> void resolve(FutureResolved<T> resolved, T result);

    /**
     * Run failed handle on {@code FutureFailed}.
     *
     * @param failed The handle to run.
     * @param cause The error thrown.
     * @see FutureResolved#fail(Throwable)
     */
    public void fail(FutureFailed failed, Throwable cause);

    /**
     * Run resolved handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle.
     * @param result Result to provide to collector.
     * @see StreamCollector#resolved(T)
     */
    public <T, R> void resolve(StreamCollector<T, R> collector, T result);

    /**
     * Run failed handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle on.
     * @param cause Error to provide to collector.
     * @see StreamCollector#failed(Throwable)
     */
    public <T, R> void fail(StreamCollector<T, R> collector, Throwable cause);

    /**
     * Run cancelled handle on {@code StreamCollector}.
     *
     * @param collector Collector to run handle on.
     * @see StreamCollector#cancelled()
     */
    public <T, R> void cancel(StreamCollector<T, R> collector);
}