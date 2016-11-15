package eu.toolchain.concurrent;

public class ConcurrentCompletableImmediateResolvedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      Caller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletable<From> future = new ConcurrentCompletable<>(caller);
    future.complete(result);
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.COMPLETED;
  }
}
