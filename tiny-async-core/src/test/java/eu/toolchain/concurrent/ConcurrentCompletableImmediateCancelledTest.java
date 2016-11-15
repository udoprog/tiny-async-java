package eu.toolchain.concurrent;

public class ConcurrentCompletableImmediateCancelledTest
    extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletable<From> future = new ConcurrentCompletable<>(caller);
    future.cancel();
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.CANCELLED;
  }
}
