package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThenCatchCancelledHelper<T> implements CompletionHandle<T> {
  private final Supplier<? extends T> transform;
  private final CompletableFuture<T> target;

  @Override
  public void failed(Throwable cause) throws Exception {
    target.fail(cause);
  }

  @Override
  public void resolved(T result) throws Exception {
    target.complete(result);
  }

  @Override
  public void cancelled() throws Exception {
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
