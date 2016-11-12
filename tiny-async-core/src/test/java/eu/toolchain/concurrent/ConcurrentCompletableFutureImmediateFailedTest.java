package eu.toolchain.concurrent;

public class ConcurrentCompletableFutureImmediateFailedTest extends ImmediateAsyncFutureTestBase {
  @Override
  protected AbstractImmediate<From> setupFuture(
      FutureCaller caller, From result, Throwable cause
  ) {
    final ConcurrentCompletableFuture<From> future = new ConcurrentCompletableFuture<>(caller);
    future.fail(cause);
    return future;
  }

  @Override
  protected ExpectedState setupState() {
    return ExpectedState.FAILED;
  }
}
