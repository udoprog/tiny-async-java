package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExecutorFutureCaller implements FutureCaller {
  private final ExecutorService executor;
  private final FutureCaller caller;

  @Override
  public <T> void complete(final CompletionHandle<T> handle, final T result) {
    executor.execute(() -> caller.complete(handle, result));
  }

  @Override
  public <T> void fail(final CompletionHandle<T> handle, final Throwable error) {
    executor.execute(() -> caller.fail(handle, error));
  }

  @Override
  public <T> void cancel(final CompletionHandle<T> handle) {
    executor.execute(() -> caller.cancel(handle));
  }

  @Override
  public void cancel(final Runnable runnable) {
    executor.execute(() -> caller.cancel(runnable));
  }

  @Override
  public void finish(final Runnable runnable) {
    executor.execute(() -> caller.finish(runnable));
  }

  @Override
  public <T> void complete(final Consumer<T> consumer, final T value) {
    executor.execute(() -> caller.complete(consumer, value));
  }

  @Override
  public <T, R> void complete(final StreamCollector<T, R> collector, final T result) {
    executor.execute(() -> caller.complete(collector, result));
  }

  @Override
  public <T, R> void fail(final StreamCollector<T, R> collector, final Throwable error) {
    executor.execute(() -> caller.fail(collector, error));
  }

  @Override
  public <T, R> void cancel(final StreamCollector<T, R> collector) {
    executor.execute(() -> caller.cancel(collector));
  }

  @Override
  public void fail(final Consumer<? super Throwable> consumer, final Throwable cause) {
    executor.execute(() -> caller.fail(consumer, cause));
  }

  @Override
  public <T> void referenceLeaked(final T reference, final StackTraceElement[] stack) {
    executor.execute(() -> caller.referenceLeaked(reference, stack));
  }
}
