package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.TinyThrowableUtils;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link eu.toolchain.concurrent.FutureFramework#collectAndDiscard(Collection)}.
 *
 * @author udoprog
 */
public class CollectAndDiscardHelper implements CompletionHandle<Object> {
  private final CompletableFuture<Void> target;
  private final AtomicInteger countdown;
  private final AtomicInteger cancelled = new AtomicInteger();
  private final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

  public CollectAndDiscardHelper(int size, CompletableFuture<Void> target) {
    this.target = target;
    this.countdown = new AtomicInteger(size);
  }

  @Override
  public void failed(Throwable e) throws Exception {
    errors.add(e);
    check();
  }

  @Override
  public void resolved(Object result) throws Exception {
    check();
  }

  @Override
  public void cancelled() throws Exception {
    cancelled.incrementAndGet();
    check();
  }

  private void done() {
    if (!errors.isEmpty()) {
      target.fail(TinyThrowableUtils.buildCollectedException(errors));
      return;
    }

    if (cancelled.get() > 0) {
      target.cancel();
      return;
    }

    target.complete(null);
  }

  private void check() throws Exception {
    if (countdown.decrementAndGet() == 0) {
      done();
    }
  }
}
