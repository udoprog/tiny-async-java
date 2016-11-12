package eu.toolchain.concurrent;

import java.util.function.Function;

public class TheComposeCancelledHelperTest extends TransformHelperTestBase<Void> {
  @Override
  protected CompletionHandle<Object> setupDone(
      Function<Void, CompletionStage<Object>> transform, CompletableFuture<Object> target
  ) {
    return new TheComposeCancelledHelper<>(() -> transform.apply(null), target);
  }

  @Override
  protected Void setupFrom() {
    return null;
  }

  @Override
  protected int setupCancelled() {
    return 1;
  }
}
