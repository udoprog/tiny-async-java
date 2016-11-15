package eu.toolchain.concurrent;

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
 *
 * <p>retry-until-completed is provided by
 * {@link Async#retryUntilCompleted(java.util.concurrent.Callable, RetryPolicy)}.
 *
 * @param <T> result type of the retried operation
 */
public class RetryCallHelper<T> implements CompletionHandle<T> {
  private final long start;
  private final ScheduledExecutorService scheduler;
  private final Callable<? extends Stage<? extends T>> action;
  private final Supplier<RetryDecision> policyInstance;
  private final Completable<T> future;
  private final ClockSource clockSource;

  /*
   * Does not require synchronization since the behaviour of this helper guarantees that only
   * one thread at a time accesses it
   */
  private final ArrayList<RetryException> errors = new ArrayList<>();
  private final AtomicReference<ScheduledFuture<?>> nextCall = new AtomicReference<>();

  public RetryCallHelper(
      final long start, final ScheduledExecutorService scheduler,
      final Callable<? extends Stage<? extends T>> callable,
      final Supplier<RetryDecision> policyInstance, final Completable<T> future,
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
  public void failed(final Throwable cause) {
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
  public void completed(final T result) {
    future.complete(result);
  }

  @Override
  public void cancelled() {
    future.cancel();
  }

  public void next() {
    if (future.isDone()) {
      throw new IllegalStateException("Target completable is done");
    }

    final Stage<? extends T> result;

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

    result.whenDone(this);
  }

  /**
   * Must be called when the target completable finishes to clean up any potential scheduled _future_
   * events.
   */
  public void finished() {
    final ScheduledFuture<?> scheduled = nextCall.getAndSet(null);

    if (scheduled != null) {
      scheduled.cancel(true);
    }
  }
}
