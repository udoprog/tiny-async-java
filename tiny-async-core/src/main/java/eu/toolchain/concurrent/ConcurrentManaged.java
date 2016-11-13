package eu.toolchain.concurrent;

import static eu.toolchain.concurrent.CoreAsync.formatStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

public class ConcurrentManaged<T> implements Managed<T> {
  private static final boolean TRACING;
  private static final boolean CAPTURE_STACK;

  // fetch and compare the value of properties that modifies runtime behaviour of this class.
  static {
    TRACING = "on".equals(System.getProperty(Managed.TRACING, "off"));
    CAPTURE_STACK = "on".equals(System.getProperty(Managed.CAPTURE_STACK, "off"));
  }

  private static final InvalidBorrowed<?> INVALID = new InvalidBorrowed<>();
  private static final StackTraceElement[] EMPTY_STACK = new StackTraceElement[0];

  private final FutureCaller caller;
  private final Supplier<? extends CompletionStage<T>> setup;

  // the managed reference.
  final AtomicReference<T> reference = new AtomicReference<>();

  // acts to allow only a single thread to setup the reference.
  private final CompletableFuture<Void> startFuture;
  private final CompletableFuture<Void> zeroLeaseFuture;
  private final CompletableFuture<T> stopReferenceFuture;

  // composite future that depends on zero-lease, and stop-reference.
  private final CompletionStage<Void> stopFuture;
  private final Set<ValidBorrowed> traces;

  final AtomicReference<ManagedState> state = new AtomicReference<>(ManagedState.INITIALIZED);

  /**
   * The number of borrowed references that are out in the wild.
   */
  final AtomicInteger leases = new AtomicInteger(1);

  public static <T> ConcurrentManaged<T> newManaged(
      final Async async, final FutureCaller caller,
      final Supplier<? extends CompletionStage<T>> setup,
      final Function<? super T, ? extends CompletionStage<Void>> teardown
  ) {
    final CompletableFuture<Void> startFuture = async.future();
    final CompletableFuture<Void> zeroLeaseFuture = async.future();
    final CompletableFuture<T> stopReferenceFuture = async.future();

    final CompletionStage<Void> stopFuture =
        zeroLeaseFuture.thenCompose(v -> stopReferenceFuture.thenCompose(teardown));

    return new ConcurrentManaged<>(caller, setup, startFuture, zeroLeaseFuture, stopReferenceFuture,
        stopFuture);
  }

  ConcurrentManaged(
      final FutureCaller caller, final Supplier<? extends CompletionStage<T>> setup,
      final CompletableFuture<Void> startFuture, final CompletableFuture<Void> zeroLeaseFuture,
      final CompletableFuture<T> stopReferenceFuture, final CompletionStage<Void> stopFuture
  ) {
    this.caller = caller;
    this.setup = setup;

    this.startFuture = startFuture;
    this.zeroLeaseFuture = zeroLeaseFuture;
    this.stopReferenceFuture = stopReferenceFuture;
    this.stopFuture = stopFuture;

    if (TRACING) {
      traces = Collections.newSetFromMap(new ConcurrentHashMap<ValidBorrowed, Boolean>());
    } else {
      traces = null;
    }
  }

  @Override
  public <R> CompletionStage<R> doto(
      final Function<? super T, ? extends CompletionStage<R>> action
  ) {
    final Borrowed<T> b = borrow();

    if (!b.isValid()) {
      return new ImmediateCancelled<>(caller);
    }

    final T reference = b.get();

    final CompletionStage<R> f;

    try {
      f = action.apply(reference);
    } catch (final Exception e) {
      b.release();
      return new ImmediateFailed<>(caller, e);
    }

    return f.whenFinished(b::release);
  }

  @Override
  public Borrowed<T> borrow() {
    /* pre-emptively increase the number of leases in order to prevent the underlying object
     * (if valid) from being de-allocated. */
    retain();

    final T value = reference.get();

    if (value == null) {
      release();
      return invalid();
    }

    final ValidBorrowed b = new ValidBorrowed(value, getStackTrace());

    if (traces != null) {
      traces.add(b);
    }

    return b;
  }

  @Override
  public boolean isReady() {
    return startFuture.isDone();
  }

  @Override
  public CompletionStage<Void> start() {
    if (!state.compareAndSet(ManagedState.INITIALIZED, ManagedState.STARTED)) {
      return startFuture;
    }

    final CompletionStage<T> constructor;

    try {
      constructor = setup.get();
    } catch (final Exception e) {
      return new ImmediateFailed<>(caller, e);
    }

    return constructor.<Void>thenApply(result -> {
      if (result == null) {
        throw new IllegalArgumentException("setup reference must no non-null");
      }

      reference.set(result);
      return null;
    }).handle(new CompletionHandle<Void>() {
      @Override
      public void failed(final Throwable cause) {
        startFuture.fail(cause);
      }

      @Override
      public void completed(Void result) {
        startFuture.complete(null);
      }

      @Override
      public void cancelled() {
        startFuture.cancel();
      }
    });
  }

  @Override
  public CompletionStage<Void> stop() {
    if (!state.compareAndSet(ManagedState.STARTED, ManagedState.STOPPED)) {
      return stopFuture;
    }

    stopReferenceFuture.complete(this.reference.getAndSet(null));

    // release self-reference.
    release();
    return stopFuture;
  }

  void retain() {
    leases.incrementAndGet();
  }

  void release() {
    final int lease = leases.decrementAndGet();

    if (lease == 0) {
      zeroLeaseFuture.complete(null);
    }
  }

  @Override
  public String toString() {
    final T reference = this.reference.get();

    if (traces == null) {
      return String.format("Managed(%s, %s)", state, reference);
    }

    return toStringTracing(reference, new ArrayList<>(traces));
  }

  String toStringTracing(final T reference, List<ValidBorrowed> traces) {
    final StringBuilder builder = new StringBuilder();

    builder.append(String.format("Managed(%s, %s:\n", state, reference));

    int i = 0;

    for (final ValidBorrowed b : traces) {
      builder.append(String.format("#%d\n", i++));
      builder.append(formatStack(b.stack()) + "\n");
    }

    builder.append(")");
    return builder.toString();
  }

  StackTraceElement[] getStackTrace() {
    if (!CAPTURE_STACK) {
      return EMPTY_STACK;
    }

    final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    return Arrays.copyOfRange(stack, 0, stack.length - 2);
  }

  @SuppressWarnings("unchecked")
  static <T> Borrowed<T> invalid() {
    return (Borrowed<T>) INVALID;
  }

  static class InvalidBorrowed<T> implements Borrowed<T> {
    @Override
    public void close() {
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public T get() {
      throw new IllegalStateException("cannot get an invalid borrowed reference");
    }

    @Override
    public void release() {
    }
  }

  /**
   * Wraps returned references that are taken from this SetupOnce instance.
   */
  @RequiredArgsConstructor
  class ValidBorrowed implements Borrowed<T> {
    private final T reference;
    protected final StackTraceElement[] stack;

    final AtomicBoolean released = new AtomicBoolean(false);

    @Override
    public T get() {
      return reference;
    }

    @Override
    public void release() {
      if (!released.compareAndSet(false, true)) {
        return;
      }

      if (traces != null) {
        traces.remove(this);
      }

      ConcurrentManaged.this.release();
    }

    @Override
    public void close() {
      release();
    }

    /**
     * Implement to log errors on release errors.
     */
    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      if (released.get()) {
        return;
      }

      caller.referenceLeaked(reference, stack);
    }

    @Override
    public boolean isValid() {
      return true;
    }

    StackTraceElement[] stack() {
      return stack;
    }
  }

  enum ManagedState {
    INITIALIZED, STARTED, STOPPED
  }
}
