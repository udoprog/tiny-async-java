package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenComposeHelper<S, T> implements CompletionHandle<S> {
  private final Function<? super S, ? extends CompletionStage<T>> transform;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable e) throws Exception {
    target.fail(e);
  }

  @Override
  public void resolved(S result) throws Exception {
    final CompletionStage<? extends T> t;

    try {
      t = transform.apply(result);
    } catch (Exception e) {
      failed(e);
      return;
    }

    t.handle(new CompletionHandle<T>() {
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
  public void cancelled() throws Exception {
    target.cancel();
  }
}
