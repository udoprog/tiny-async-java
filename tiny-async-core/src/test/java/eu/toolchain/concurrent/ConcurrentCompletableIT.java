package eu.toolchain.concurrent;

public class ConcurrentCompletableIT extends AbstractCompletableIT {
  @Override
  protected <T> Completable<T> setupFuture(final FutureCaller caller) {
    return new ConcurrentCompletable<>(caller);
  }
}
