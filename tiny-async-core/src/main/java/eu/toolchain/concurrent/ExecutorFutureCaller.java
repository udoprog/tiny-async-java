package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExecutorFutureCaller implements FutureCaller {
  private final ExecutorService executor;
  private final FutureCaller caller;

  @Override
  public <T> void resolve(final CompletionHandle<T> handle, final T result) {
    executor.execute(() -> caller.resolve(handle, result));
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
  public void cancel(final Runnable cancelled) {
    executor.execute(() -> caller.cancel(cancelled));
  }

  @Override
  public void finish(final Runnable finishable) {
    executor.execute(() -> caller.finish(finishable));
  }

  @Override
  public <T> void resolve(final Consumer<T> resolved, final T value) {
    executor.execute(() -> caller.resolve(resolved, value));
  }

  @Override
  public <T, R> void resolve(final StreamCollector<T, R> collector, final T result) {
    executor.execute(() -> caller.resolve(collector, result));
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
  public void fail(final Consumer<? super Throwable> failed, final Throwable cause) {
    executor.execute(() -> caller.fail(failed, cause));
  }

  @Override
  public <T> void referenceLeaked(final T reference, final StackTraceElement[] stack) {
    executor.execute(() -> caller.referenceLeaked(reference, stack));
  }
}
