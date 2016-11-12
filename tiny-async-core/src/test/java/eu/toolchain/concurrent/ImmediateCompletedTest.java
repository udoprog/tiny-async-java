package eu.toolchain.concurrent;

public class ImmediateCompletedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      FutureCaller caller, From result, Throwable cause
  ) {
    return new ImmediateCompleted<>(caller, result);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.COMPLETED;
  }
}
