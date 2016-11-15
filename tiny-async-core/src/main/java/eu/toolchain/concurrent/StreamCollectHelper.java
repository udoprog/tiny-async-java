package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper for {@link Async#streamCollect(Collection, StreamCollector)}.
 *
 * @param <S> the source type being collected
 * @param <T> The type the source type is being collected and transformed into
 * @author udoprog
 */
class StreamCollectHelper<S, T> implements CompletionHandle<S> {
  private final Caller caller;
  private final StreamCollector<S, T> collector;
  private final Completable<? super T> target;

  private final AtomicInteger countdown;
  private final AtomicInteger successful;
  private final AtomicInteger failed;
  private final AtomicInteger cancelled;

  StreamCollectHelper(
      final Caller caller, final int size, final StreamCollector<S, T> collector,
      final Completable<? super T> target
  ) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }

    this.caller = caller;
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
    caller.execute(() -> collector.failed(e));
    check();
  }

  @Override
  public void completed(S result) {
    successful.incrementAndGet();
    caller.execute(() -> collector.completed(result));
    check();
  }

  @Override
  public void cancelled() {
    cancelled.incrementAndGet();
    caller.execute(collector::cancelled);
    check();
  }

  private void check() {
    if (countdown.decrementAndGet() == 0) {
      try {
        target.complete(collector.end(successful.get(), failed.get(), cancelled.get()));
      } catch (Exception e) {
        target.fail(e);
      }
    }
  }
}
