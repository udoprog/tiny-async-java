package eu.toolchain.concurrent;

import static eu.toolchain.concurrent.CoreAsync.buildCollectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * Helper class for {@link CoreAsync#collect(Collection, Function)}
 *
 * <p>The helper implements {@code CompletionHandle}, and is intended to be used by binding it as a
 * listener to the futures being collected.
 *
 * <p>This is a lock-free implementation capable of writing the results out of order.
 *
 * @param <T> the source type being collected
 * @param <U> the collected value
 */
class CollectHelper<T, U> implements CompletionHandle<T> {
  static final byte RESOLVED = 0x1;
  static final byte FAILED = 0x2;
  static final byte CANCELLED = 0x3;

  final Function<? super Collection<T>, ? extends U> collector;
  Collection<? extends Stage<?>> sources;
  final Completable<? super U> target;
  final int size;

  /* The collected results, non-final to allow for setting to null. Allows for random writes
   * since its a pre-emptively sized array. */
  Object[] values;
  byte[] states;

  /* maintain position separate since the is a potential race condition between getting the
   * current position and setting the entry. This is avoided by only relying on countdown to trigger
   * when we are done. */
  final AtomicInteger write = new AtomicInteger();

  /* maintain a separate countdown since the write position might be out of order, this causes all
   * threads to synchronize after the write */
  final AtomicInteger countdown;

  /* Indicate that collector is finished to avoid the case where the write position wraps around. */
  final AtomicBoolean finished = new AtomicBoolean();

  /* On a single failure, cause all other sources to be cancelled */
  final AtomicBoolean failed = new AtomicBoolean();

  CollectHelper(
      int size, Function<? super Collection<T>, ? extends U> collector,
      Collection<? extends Stage<?>> sources, Completable<? super U> target
  ) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }

    this.size = size;
    this.collector = collector;
    this.sources = sources;
    this.target = target;
    this.values = new Object[size];
    this.states = new byte[size];
    this.countdown = new AtomicInteger(size);
  }

  @Override
  public void completed(T result) {
    add(RESOLVED, result);
  }

  @Override
  public void failed(Throwable e) {
    add(FAILED, e);
    checkFailed();
  }

  @Override
  public void cancelled() {
    add(CANCELLED, null);
    checkFailed();
  }

  void checkFailed() {
    if (!failed.compareAndSet(false, true)) {
      return;
    }

    for (final Stage<?> source : sources) {
      source.cancel();
    }

    // help garbage collection.
    sources = null;
  }

  /**
   * Checks in a doCall back. It also wraps up the group if all the callbacks have checked in.
   */
  void add(final byte type, final Object value) {
    if (finished.get()) {
      throw new IllegalStateException("already finished");
    }

    final int w = write.getAndIncrement();

    if (w < size) {
      writeAt(w, type, value);
    }

    // countdown could wrap around, however we check the state of finished in here.
    // MUST be called after write to make sure that results and states are synchronized.
    final int c = countdown.decrementAndGet();

    if (c < 0) {
      throw new IllegalStateException("already finished (countdown)");
    }

    // if this thread is not the last thread to check-in, do nothing..
    if (c != 0) {
      return;
    }

    // make sure this can only happen once.
    // This protects against countdown, and write wrapping around which should very rarely
    // happen.
    if (!finished.compareAndSet(false, true)) {
      throw new IllegalStateException("already finished");
    }

    done(collect());
  }

  void writeAt(final int w, final byte state, final Object value) {
    states[w] = state;
    values[w] = value;
  }

  void done(Results r) {
    final Collection<T> results = r.results;
    final Collection<Throwable> errors = r.errors;
    final int cancelled = r.cancelled;

    if (!errors.isEmpty()) {
      target.fail(buildCollectedException(errors));
      return;
    }

    if (cancelled > 0) {
      target.cancel();
      return;
    }

    U result;

    try {
      result = collector.apply(results);
    } catch (final Exception error) {
      target.fail(error);
      return;
    }

    target.complete(result);
  }

  @SuppressWarnings("unchecked")
  Results collect() {
    final List<T> results = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();
    int cancelled = 0;

    for (int i = 0; i < size; i++) {
      final byte type = states[i];

      switch (type) {
        case RESOLVED:
          results.add((T) values[i]);
          break;
        case FAILED:
          errors.add((Throwable) values[i]);
          break;
        case CANCELLED:
          cancelled++;
          break;
        default:
          throw new IllegalArgumentException("Invalid entry type: " + type);
      }
    }

    // help garbage collector
    this.states = null;
    this.values = null;

    return new Results(results, errors, cancelled);
  }

  @RequiredArgsConstructor
  class Results {
    private final List<T> results;
    private final List<Throwable> errors;
    private final int cancelled;
  }
}
