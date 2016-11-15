package eu.toolchain.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper for {@link Async#endCollect(java.util.Collection, EndCollector)}.
 *
 * @param <T> the type being collected
 */
public class EndCollectHelper<T> implements CompletionHandle<Object> {
  private final FutureCaller caller;
  private final EndCollector<T> collector;
  private final Completable<? super T> target;
  private final AtomicInteger countdown;

  private final AtomicInteger successful = new AtomicInteger();
  private final AtomicInteger failed = new AtomicInteger();
  private final AtomicInteger cancelled = new AtomicInteger();

  public EndCollectHelper(
      final FutureCaller caller, final int size, final EndCollector<T> collector,
      final Completable<? super T> target
  ) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }

    this.caller = caller;
    this.collector = collector;
    this.target = target;
    this.countdown = new AtomicInteger(size);
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
