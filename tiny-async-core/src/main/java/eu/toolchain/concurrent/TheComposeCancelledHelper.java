package eu.toolchain.concurrent;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TheComposeCancelledHelper<T> implements CompletionHandle<T> {
  private final Supplier<? extends CompletionStage<T>> transform;
  private final CompletableFuture<T> target;

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
    final CompletionStage<? extends T> future;

    try {
      future = transform.get();
    } catch (Exception e) {
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
}
