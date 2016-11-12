package eu.toolchain.concurrent;

import static eu.toolchain.concurrent.CoreAsync.formatStack;

import java.util.function.Consumer;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectFutureCaller implements FutureCaller {
  @Override
  public <T> void complete(final CompletionHandle<T> handle, final T result) {
    try {
      handle.resolved(result);
    } catch (final Exception e) {
      internalError("CompletionHandle#completed(T)", e);
    }
  }

  @Override
  public <T> void fail(CompletionHandle<T> handle, Throwable error) {
    try {
      handle.failed(error);
    } catch (final Exception e) {
      internalError("CompletionHandle#failed(Throwable)", e);
    }
  }

  @Override
  public <T> void cancel(CompletionHandle<T> handle) {
    try {
      handle.cancelled();
    } catch (final Exception e) {
      internalError("CompletionHandle#cancelled()", e);
    }
  }

  @Override
  public void finish(Runnable runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      internalError("FutureFinished#finished()", e);
    }
  }

  @Override
  public void cancel(Runnable runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      internalError("FutureCancelled#cancelled()", e);
    }
  }

  @Override
  public <T> void complete(Consumer<T> consumer, T value) {
    try {
      consumer.accept(value);
    } catch (final Exception e) {
      internalError("FutureResolved#completed(T)", e);
    }
  }

  @Override
  public void fail(Consumer<? super Throwable> consumer, Throwable cause) {
    try {
      consumer.accept(cause);
    } catch (final Exception e) {
      internalError("FutureFailed#failed(Throwable)", e);
    }
  }

  @Override
  public <S, T> void complete(StreamCollector<S, T> collector, S result) {
    try {
      collector.completed(result);
    } catch (final Exception e) {
      internalError("StreamCollector#completed(T)", e);
    }
  }

  @Override
  public <S, T> void fail(StreamCollector<S, T> collector, Throwable error) {
    try {
      collector.failed(error);
    } catch (final Exception e) {
      internalError("StreamCollector#failed(Throwable)", e);
    }
  }

  @Override
  public <S, T> void cancel(StreamCollector<S, T> collector) {
    try {
      collector.cancelled();
    } catch (final Exception e) {
      internalError("StreamCollector#cancel()", e);
    }
  }

  @Override
  public <T> void referenceLeaked(T reference, StackTraceElement[] stack) {
    internalError(String.format("reference %s leaked @ %s", reference, formatStack(stack)), null);
  }

  abstract protected void internalError(String what, Throwable e);
}
