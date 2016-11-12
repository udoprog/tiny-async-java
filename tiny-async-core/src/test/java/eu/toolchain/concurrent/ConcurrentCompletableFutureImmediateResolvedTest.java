package eu.toolchain.concurrent;

public class ConcurrentCompletableFutureImmediateResolvedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      FutureCaller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletableFuture<From> future = new ConcurrentCompletableFuture<>(caller);
    future.complete(result);
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.COMPLETED;
  }
}
