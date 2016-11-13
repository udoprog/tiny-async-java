package eu.toolchain.concurrent;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenApplyHelper<S, T> implements CompletionHandle<S> {
  private final Function<? super S, ? extends T> transform;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable cause) {
    target.fail(cause);
  }

  @Override
  public void completed(S result) {
    final T value;

    try {
      value = transform.apply(result);
    } catch (Exception e) {
      target.fail(e);
      return;
    }

    target.complete(value);
  }

  @Override
  public void cancelled() {
    target.cancel();
  }
}
