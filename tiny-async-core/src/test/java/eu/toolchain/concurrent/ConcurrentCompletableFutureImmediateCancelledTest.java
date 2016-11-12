package eu.toolchain.concurrent;

public class ConcurrentCompletableFutureImmediateCancelledTest
    extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      FutureCaller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletableFuture<From> future = new ConcurrentCompletableFuture<>(caller);
    future.cancel();
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.CANCELLED;
  }
}
