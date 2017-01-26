package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Coordinator thread for handling delayed callables executing with a given parallelism.
 *
 * @param <S> The source type being collected.
 * @param <T> The target type the source type is being collected into.
 */
public class DelayedCollectCoordinator<S, T> implements Handle<S>, Runnable {
  private final AtomicInteger pending = new AtomicInteger();

  /* lock that must be acquired before using {@link callables} */
  private final Object lock = new Object();

  private final Caller caller;
  private final Iterator<? extends Callable<? extends Stage<? extends S>>> callables;
  private final Consumer<? super S> consumer;
  private final Supplier<? extends T> supplier;
  private final Completable<? super T> future;
  private final int parallelism;
  private final int total;

  volatile boolean cancel = false;
  volatile boolean done = false;

  public DelayedCollectCoordinator(
      final Caller caller,
      final Collection<? extends Callable<? extends Stage<? extends S>>> callables,
      final Consumer<S> consumer, Supplier<T> supplier, final Completable<? super T> future,
      int parallelism
  ) {
    this.caller = caller;
    this.callables = callables.iterator();
    this.consumer = consumer;
    this.supplier = supplier;
    this.future = future;
    this.parallelism = parallelism;
    this.total = callables.size();
  }

  @Override
  public void failed(Throwable cause) {
    pending.decrementAndGet();
    cancel = true;
    checkNext();
  }

  @Override
  public void completed(S result) {
    caller.execute(() -> consumer.accept(result));
    pending.decrementAndGet();
    checkNext();
  }

  @Override
  public void cancelled() {
    pending.decrementAndGet();
    cancel = true;
    checkNext();
  }

  // coordinate thread.
  @Override
  public void run() {
    synchronized (lock) {
      if (!callables.hasNext()) {
        checkEnd();
        return;
      }

      for (int i = 0; i < parallelism && callables.hasNext(); i++) {
        setupNext(callables.next());
      }
    }

    future.whenCancelled(() -> {
      cancel = true;
      checkNext();
    });
  }

  private void checkNext() {
    final Callable<? extends Stage<? extends S>> next;

    synchronized (lock) {
      if (cancel) {
        checkEnd();
        return;
      }

      if (!callables.hasNext()) {
        checkEnd();
        return;
      }

      next = callables.next();
    }

    setupNext(next);
  }

  private void setupNext(final Callable<? extends Stage<? extends S>> next) {
    final Stage<? extends S> f;

    pending.incrementAndGet();

    try {
      f = next.call();
    } catch (final Exception e) {
      failed(e);
      return;
    }

    f.handle(this);
  }

  private void checkEnd() {
    if (pending.get() > 0) {
      return;
    }

    if (done) {
      return;
    }

    done = true;

    final T value;

    try {
      value = supplier.get();
    } catch (Exception e) {
      future.fail(e);
      return;
    }

    future.complete(value);
  }
}
