package eu.toolchain.concurrent;

public class ConcurrentCompletableImmediateFailedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletable<From> future = new ConcurrentCompletable<>(caller);
    future.fail(cause);
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.FAILED;
  }
}
