package eu.toolchain.concurrent;

public class ConcurrentCompletableFutureIT extends AbstractCompletableFutureIT {
  @Override
  protected <T> CompletableFuture<T> setupFuture(final FutureCaller caller) {
    return new ConcurrentCompletableFuture<>(caller);
  }
}
