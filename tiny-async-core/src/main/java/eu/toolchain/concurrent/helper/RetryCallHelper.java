package eu.toolchain.concurrent.helper;

import eu.toolchain.concurrent.ClockSource;
import eu.toolchain.concurrent.CompletableFuture;
import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.RetryDecision;
import eu.toolchain.concurrent.RetryException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A helper class for retry-until-completed behaviour.
 * <p>
 * retry-until-completed is provided by {@link eu.toolchain.concurrent
 * .FutureFramework#retryUntilCompleted(java.util.concurrent.Callable,
 * eu.toolchain.async.RetryPolicy)}.
 *
 * @param <T> The type of the class.
 */
public class RetryCallHelper<T> implements CompletionHandle<T> {
  private final long start;
  private final ScheduledExecutorService scheduler;
  private final Callable<? extends CompletionStage<? extends T>> action;
  private final Supplier<RetryDecision> policyInstance;
  private final CompletableFuture<T> future;
  private final ClockSource clockSource;

  /*
   * Does not require synchronization since the behaviour of this helper guarantees that only
   * one thread at a time accesses it
   */
  private final ArrayList<RetryException> errors = new ArrayList<>();
  private final AtomicReference<ScheduledFuture<?>> nextCall = new AtomicReference<>();

  public RetryCallHelper(
      final long start, final ScheduledExecutorService scheduler,
      final Callable<? extends CompletionStage<? extends T>> callable,
      final Supplier<RetryDecision> policyInstance, final CompletableFuture<T> future,
      final ClockSource clockSource
  ) {
    this.start = start;
    this.scheduler = scheduler;
    this.action = callable;
    this.policyInstance = policyInstance;
    this.future = future;
    this.clockSource = clockSource;
  }

  public List<RetryException> getErrors() {
    return errors;
  }

  @Override
  public void failed(final Throwable cause) throws Exception {
    final RetryDecision decision = policyInstance.get();

    if (!decision.shouldRetry()) {
      for (final Throwable suppressed : errors) {
        cause.addSuppressed(suppressed);
      }

      future.fail(cause);
      return;
    }

    errors.add(new RetryException(clockSource.now() - start, cause));

    if (decision.backoff() <= 0) {
      next();
    } else {
      nextCall.set(scheduler.schedule(() -> {
        nextCall.set(null);
        next();
      }, decision.backoff(), TimeUnit.MILLISECONDS));
    }
  }

  @Override
  public void resolved(final T result) throws Exception {
    future.complete(result);
  }

  @Override
  public void cancelled() throws Exception {
    future.cancel();
  }

  public void next() {
    if (future.isDone()) {
      throw new IllegalStateException("Target future is done");
    }

    final CompletionStage<? extends T> result;

    try {
      result = action.call();
    } catch (final Exception e) {
      // inner catch, since the policy might be user-provided.
      try {
        failed(e);
      } catch (final Exception inner) {
        inner.addSuppressed(e);
        future.fail(inner);
      }

      return;
    }

    if (result == null) {
      future.fail(new IllegalStateException("Retry action returned null"));
      return;
    }

    result.handle(this);
  }

  /**
   * Must be called when the target future finishes to clean up any potential scheduled _future_
   * events.
   */
  public void finished() {
    final ScheduledFuture<?> scheduled = nextCall.getAndSet(null);

    if (scheduled != null) {
      scheduled.cancel(true);
    }
  }
}
