package eu.toolchain.concurrent;

public class ImmediateCancelledTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  ) {
    return new ImmediateCancelled<>(caller);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.CANCELLED;
  }
}
