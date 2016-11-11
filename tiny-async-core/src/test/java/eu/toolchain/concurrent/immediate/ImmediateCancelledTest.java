package eu.toolchain.concurrent.immediate;

import eu.toolchain.concurrent.AbstractImmediate;
import eu.toolchain.concurrent.FutureCaller;
import eu.toolchain.concurrent.ImmediateAsyncFutureTestBase;

public class ImmediateCancelledTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      FutureCaller caller, From result, Throwable cause
  ) {
    return new ImmediateCancelled<>(caller);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.CANCELLED;
  }
}
