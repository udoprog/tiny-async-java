package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link FutureFramework#collect(Collection, StreamCollector)}.
 *
 * @param <S> the source type being collected.
 * @param <T> The type the source type is being collected and transformed into.
 * @author udoprog
 */
public class CollectStreamHelper<S, T> implements CompletionHandle<S> {
  private final FutureCaller caller;
  private final StreamCollector<S, T> collector;
  private final CompletableFuture<? super T> target;
  private final AtomicInteger countdown;

  private final AtomicInteger successful = new AtomicInteger();
  private final AtomicInteger failed = new AtomicInteger();
  private final AtomicInteger cancelled = new AtomicInteger();

  public CollectStreamHelper(
      final FutureCaller caller, final int size, final StreamCollector<S, T> collector,
      final CompletableFuture<? super T> target
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
  public void failed(Throwable e) throws Exception {
    failed.incrementAndGet();
    caller.fail(collector, e);
    check();
  }

  @Override
  public void resolved(S result) throws Exception {
    successful.incrementAndGet();
    caller.complete(collector, result);
    check();
  }

  @Override
  public void cancelled() throws Exception {
    cancelled.incrementAndGet();
    caller.cancel(collector);
    check();
  }

  private void check() throws Exception {
    if (countdown.decrementAndGet() == 0) {
      done();
    }
  }

  private void done() {
    final T result;

    try {
      result = collector.end(successful.get(), failed.get(), cancelled.get());
    } catch (Exception e) {
      target.fail(e);
      return;
    }

    target.complete(result);
  }
}
