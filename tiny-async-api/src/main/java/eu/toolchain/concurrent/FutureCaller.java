package eu.toolchain.concurrent;

import java.util.function.Consumer;

/**
 * User-defined functions to handle unexpected circumstances.
 * <p>
 * The implementation of these methods will be invoked from the calling thread that interacts with
 * the future.
 * <p>
 * None of the below methods throw checked exceptions, and they are intended to never throw
 * anything, with the exception of {@code Error}. This means that the implementor is required to
 * make sure this doesn't happen, the best way to accomplish this is to wrap each callback in a
 * try-catch statement like below.
 * <p>
 * <pre>
 * {@code
 * new FutureCaller() {
 *   public <T> void complete(CompletionHandle<T> handle, T result) {
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
 * <p>
 * The core of the framework provides some base classes for easily accomplishing this, most notable
 * is {@code DirectAsyncCaller}.
 *
 * @author udoprog
 */
public interface FutureCaller {
  /**
   * Indicate that a Managed reference has been leaked.
   *
   * @param reference The reference that was leaked.
   * @param stack The stacktrace for where it was leaked, can be {@code null} if information is
   * unavailable.
   * @param <T> the type of the reference being leaked.
   */
  <T> void referenceLeaked(T reference, StackTraceElement[] stack);

  /**
   * Run resolved handle on {@code CompletionHandle}.
   *
   * @param handle The handle to run.
   * @param result The result that resolved the future.
   * @param <T> type of the handle.
   * @see CompletionHandle#resolved(Object)
   */
  <T> void resolve(CompletionHandle<T> handle, T result);

  /**
   * Run failed handle on {@code CompletionHandle}.
   *
   * @param handle The handle to run.
   * @param cause The cause of the failure.
   * @param <T> the type of the handle.
   * @see CompletionHandle#failed(Throwable)
   */
  <T> void fail(CompletionHandle<T> handle, Throwable cause);

  /**
   * Run cancelled handle on {@code CompletionHandle}.
   *
   * @param handle The handle to run on.
   * @param <T> type of the handle.
   * @see CompletionHandle#cancelled()
   */
  <T> void cancel(CompletionHandle<T> handle);

  /**
   * Run finished handle on {@code FutureFinished}.
   *
   * @param finishable The handle to run.
   */
  void finish(Runnable finishable);

  /**
   * Run cancelled handle on {@code FutureCancelled}.
   *
   * @param cancelled The handle to run on.
   */
  void cancel(Runnable cancelled);

  /**
   * Run resolved handle on {@code FutureResolved<T>}.
   *
   * @param resolved The handle to run.
   * @param result The result to complete the future.
   * @param <T> type of the resolved value.
   */
  <T> void resolve(Consumer<T> resolved, T result);

  /**
   * Run failed handle on {@code FutureFailed}.
   *
   * @param failed The handle to run.
   * @param cause The error thrown.
   */
  void fail(Consumer<? super Throwable> failed, Throwable cause);

  /**
   * Run resolved handle on {@code StreamCollector}.
   *
   * @param collector Collector to run handle.
   * @param result Result to provide to collector.
   * @param <S> source type of the collector.
   * @param <T> target type of the collector.
   * @see StreamCollector#resolved(Object)
   */
  <S, T> void resolve(StreamCollector<S, T> collector, S result);

  /**
   * Run failed handle on {@code StreamCollector}.
   *
   * @param collector Collector to run handle on.
   * @param cause Error to provide to collector.
   * @param <S> source type of the collector.
   * @param <T> target type of the collector.
   * @see StreamCollector#failed(Throwable)
   */
  <S, T> void fail(StreamCollector<S, T> collector, Throwable cause);

  /**
   * Run cancelled handle on {@code StreamCollector}.
   *
   * @param collector Collector to run handle on.
   * @param <S> source type of the collector.
   * @param <T> target type of the collector.
   * @see StreamCollector#cancelled()
   */
  <S, T> void cancel(StreamCollector<S, T> collector);
}
