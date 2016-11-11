package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediateCompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.FutureFramework;
import eu.toolchain.concurrent.ImmediateAsyncFutureTestBase;

public class ImmediateFailedStageTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediateCompletionStage<From> setupFuture(
      FutureFramework async, FutureCaller caller, From result, Throwable cause
  ) {
    return new ImmediateFailedStage<>(async, caller, cause);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.FAILED;
  }
}
