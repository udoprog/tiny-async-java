package eu.toolchain.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper for {@link Async#endCollect(java.util.Collection, EndCollector)}.
 *
 * @param <T> the type being collected
 */
class EndCollectHelper<T> implements CompletionHandle<Object> {
  private final EndCollector<T> collector;
  private final Completable<? super T> target;

  private final AtomicInteger countdown;
  private final AtomicInteger successful;
  private final AtomicInteger failed;
  private final AtomicInteger cancelled;

  EndCollectHelper(
      final int size, final EndCollector<T> collector, final Completable<? super T> target
  ) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }

    this.collector = collector;
    this.target = target;

    this.countdown = new AtomicInteger(size);
    this.successful = new AtomicInteger();
    this.failed = new AtomicInteger();
    this.cancelled = new AtomicInteger();
  }

  @Override
  public void failed(Throwable e) {
    failed.incrementAndGet();
    check();
  }

  @Override
  public void completed(Object result) {
    successful.incrementAndGet();
    check();
  }

  @Override
  public void cancelled() {
    cancelled.incrementAndGet();
    check();
  }

  private void check() {
    if (countdown.decrementAndGet() == 0) {
      try {
        target.complete(collector.apply(successful.get(), failed.get(), cancelled.get()));
      } catch (final Exception e) {
        target.fail(e);
      }
    }
  }
}
