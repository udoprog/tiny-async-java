package eu.toolchain.concurrent;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenComposeFailedHelper<T> implements CompletionHandle<T> {
  private final Function<? super Throwable, ? extends CompletionStage<T>> fn;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable cause) {
    final CompletionStage<? extends T> future;

    try {
      future = fn.apply(cause);
    } catch (final Exception e) {
      e.addSuppressed(cause);
      target.fail(e);
      return;
    }

    future.handle(new CompletionHandle<T>() {
      @Override
      public void failed(Throwable e) {
        target.fail(e);
      }

      @Override
      public void completed(T result) {
        target.complete(result);
      }

      @Override
      public void cancelled() {
        target.cancel();
      }
    });
  }

  @Override
  public void completed(T result) {
    target.complete(result);
  }

  @Override
  public void cancelled() {
    target.cancel();
  }
}
