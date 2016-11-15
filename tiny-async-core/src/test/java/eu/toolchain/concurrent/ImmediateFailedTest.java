package eu.toolchain.concurrent;

public class ImmediateFailedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  ) {
    return new ImmediateFailed<>(caller, cause);
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.FAILED;
  }
}
