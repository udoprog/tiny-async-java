package eu.toolchain.concurrent;

public class ConcurrentCompletableIT extends AbstractCompletableIT {
  @Override
  protected <T> Completable<T> setupFuture(final Caller caller) {
    return new ConcurrentCompletable<>(caller);
  }
}
