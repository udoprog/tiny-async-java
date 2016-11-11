package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import java.util.function.Function;

public class ThenComposeHelperTest extends TransformHelperTestBase<Object> {
  private final Object from = new Object();

  @Override
  protected CompletionHandle<Object> setupDone(
      Function<Object, CompletionStage<Object>> transform, CompletableFuture<Object> target
  ) {
    return new ThenComposeHelper<Object, Object>(transform, target);
  }

  @Override
  protected Object setupFrom() {
    return from;
  }

  @Override
  protected int setupResolved() {
    return 1;
  }
}
