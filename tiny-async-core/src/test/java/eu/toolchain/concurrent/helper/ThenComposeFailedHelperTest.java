package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import java.util.function.Function;

public class ThenComposeFailedHelperTest extends TransformHelperTestBase<Throwable> {
  private final RuntimeException e = new RuntimeException();

  @Override
  protected CompletionHandle<Object> setupDone(
      Function<Throwable, CompletionStage<Object>> transform, CompletableFuture<Object> target
  ) {
    return new ThenComposeFailedHelper<>(transform, target);
  }

  @Override
  protected Throwable setupFrom() {
    return e;
  }

  @Override
  protected RuntimeException setupError() {
    return e;
  }

  @Override
  protected int setupFailed() {
    return 1;
  }
}
