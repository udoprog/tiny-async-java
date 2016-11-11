package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
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
