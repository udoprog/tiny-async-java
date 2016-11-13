package eu.toolchain.concurrent;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenCatchCancelledHelper<T> implements CompletionHandle<T> {
  private final Supplier<? extends T> transform;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable cause) {
    target.fail(cause);
  }

  @Override
  public void completed(T result) {
    target.complete(result);
  }

  @Override
  public void cancelled() {
    final T value;

    try {
      value = transform.get();
    } catch (Exception e) {
      target.fail(e);
      return;
    }

    target.complete(value);
  }
}
