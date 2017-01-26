package eu.toolchain.concurrent;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Stream collect helper.
 *
 * @param <S> the source type being collected
 * @param <T> The type the source type is being collected and transformed into
 * @author udoprog
 */
class StreamCollectHelper<S, T> implements Handle<S> {
  private final Caller caller;
  private final Consumer<S> consumer;
  private final Supplier<T> supplier;
  private final Completable<? super T> target;

  private final AtomicInteger countdown;
  private final AtomicInteger successful;
  private final AtomicInteger failed;
  private final AtomicInteger cancelled;

  private volatile boolean cancel;

  private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

  StreamCollectHelper(
      final Caller caller, final int size, final Consumer<S> consumer, final Supplier<T> supplier,
      final Completable<? super T> target
  ) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }

    this.caller = caller;
    this.consumer = consumer;
    this.supplier = supplier;
    this.target = target;

    this.countdown = new AtomicInteger(size);
    this.successful = new AtomicInteger();
    this.failed = new AtomicInteger();
    this.cancelled = new AtomicInteger();
  }

  @Override
  public void failed(Throwable e) {
    failed.incrementAndGet();
    errors.add(e);
    check();
  }

  @Override
  public void completed(S result) {
    successful.incrementAndGet();
    caller.execute(() -> consumer.accept(result));
    check();
  }

  @Override
  public void cancelled() {
    cancelled.incrementAndGet();
    cancel = true;
    check();
  }

  private void check() {
    if (countdown.decrementAndGet() == 0) {
      if (cancel) {
        target.cancel();
        return;
      }

      if (errors.size() > 0) {
        final Iterator<Throwable> it = errors.iterator();
        final Throwable first = it.next();

        while (it.hasNext()) {
          first.addSuppressed(it.next());
        }

        target.fail(first);
        return;
      }

      try {
        target.complete(supplier.get());
      } catch (Exception e) {
        target.fail(e);
      }
    }
  }
}
