package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenComposeFailedHelper<T> implements CompletionHandle<T> {
  private final Function<? super Throwable, ? extends CompletionStage<T>> fn;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable cause) throws Exception {
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
      public void failed(Throwable e) throws Exception {
        target.fail(e);
      }

      @Override
      public void resolved(T result) throws Exception {
        target.complete(result);
      }

      @Override
      public void cancelled() throws Exception {
        target.cancel();
      }
    });
  }

  @Override
  public void resolved(T result) throws Exception {
    target.complete(result);
  }

  @Override
  public void cancelled() throws Exception {
    target.cancel();
  }
}
