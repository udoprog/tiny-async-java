package eu.toolchain.concurrent;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenComposeHelper<S, T> implements CompletionHandle<S> {
  private final Function<? super S, ? extends CompletionStage<T>> transform;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable e) {
    target.fail(e);
  }

  @Override
  public void completed(S result) {
    final CompletionStage<? extends T> t;

    try {
      t = transform.apply(result);
    } catch (Exception e) {
      failed(e);
      return;
    }

    t.handle(new CompletionHandle<T>() {
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
  public void cancelled() {
    target.cancel();
  }
}
