package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediateCompletionStage;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.FutureFramework;
import eu.toolchain.concurrent.ImmediateAsyncFutureTestBase;

public class ImmediateCancelledStageTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediateCompletionStage<From> setupFuture(
      FutureFramework async, FutureCaller caller, From result, Throwable cause
  ) {
    return new ImmediateCancelledStage<From>(async, caller);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.CANCELLED;
  }
}
