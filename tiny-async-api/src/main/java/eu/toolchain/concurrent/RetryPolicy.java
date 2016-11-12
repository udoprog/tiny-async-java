package eu.toolchain.concurrent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A policy that governs how and when an operation should be retried.
 *
 * <p>Policies decide if a retry should be performed or not by expiring after some given parameters.
 *
 * <p>Policies are factories, and do not maintain any state in themselves. They can be safely
 * re-used like this:
 *
 * <pre>{@code
 *   public class Main {
 *     public static final RetryPolicy LINEAR =
 *         timed(10, TimeUnit.SECONDS, linear(10, TimeUnit.MILLISECONDS))
 *   }
 * }</pre>
 */
@FunctionalInterface
public interface RetryPolicy {
  /**
   * Create a new instance of the policy.
   *
   * <p>the provided instance may only be used by one thread at a time
   *
   * @param clockSource clock source to use in the instance
   * @return a policy instance
   */
  Supplier<RetryDecision> newInstance(ClockSource clockSource);

  /**
   * Build a linear retry policy.
   *
   * @param duration linear backoff to as a duration to apply
   * @param unit unit of duration
   * @return a new retry policy
   */
  static RetryPolicy linear(final long duration, final TimeUnit unit) {
    return new Linear(ClockSource.UNIT.convert(duration, unit));
  }

  /**
   * Setup an exponential backoff retry policy.
   *
   * @param duration the base time to backoff in milliseconds
   * @param unit unit of duration
   * @return an exponential retry policy
   */
  static ExponentialBuilder exponential(final long duration, final TimeUnit unit) {
    final long base = ClockSource.UNIT.convert(duration, unit);
    return new ExponentialBuilder(base);
  }

  /**
   * Wrap an existing retry policy which is only valid for a given time.
   *
   * <p>This allows you to provide a custom clock source.
   *
   * @param duration the duration for which the policy should be valid
   * @param unit time unit of the duration
   * @param policy the policy to wrap
   * @return a timed retry policy
   */
  static RetryPolicy timed(final long duration, final TimeUnit unit, final RetryPolicy policy) {
    return new Timed(ClockSource.UNIT.convert(duration, unit), policy);
  }

  /**
   * Implementation for the timed retry policy.
   */
  class Timed implements RetryPolicy {
    private final long duration;
    private final RetryPolicy policy;

    Timed(final long duration, final RetryPolicy policy) {
      this.duration = duration;
      this.policy = policy;
    }

    @Override
    public Supplier<RetryDecision> newInstance(ClockSource clockSource) {
      final long deadline = clockSource.now() + duration;
      final Supplier<RetryDecision> inner = policy.newInstance(clockSource);

      return () -> {
        final RetryDecision d = inner.get();
        final boolean shouldRetry = clockSource.now() < deadline && d.shouldRetry();
        return new RetryDecision(shouldRetry, d.backoff());
      };
    }

    @Override
    public String toString() {
      return "Timed(duration=" + duration + ", policy=" + policy + ")";
    }
  }

  /**
   * Implementation for the linear retry policy.
   */
  class Linear implements RetryPolicy {
    private final long backoff;

    Linear(final long backoff) {
      this.backoff = backoff;
    }

    @Override
    public Supplier<RetryDecision> newInstance(ClockSource clockSource) {
      final RetryDecision decision = new RetryDecision(true, backoff);
      return () -> decision;
    }

    @Override
    public String toString() {
      return "Linear(backoff=" + backoff + ")";
    }
  }

  /**
   * Implementation for the exponential retry policy.
   */
  class Exponential implements RetryPolicy {
    private final long base;
    private final double factor;
    private final long max;

    Exponential(final long base, final double factor, final long max) {
      this.base = base;
      this.factor = factor;
      this.max = max;
    }

    @Override
    public Supplier<RetryDecision> newInstance(ClockSource clockSource) {
      return new ExponentialInstance();
    }

    @Override
    public String toString() {
      return "Exponential(base=" + base + ", base=" + base + ")";
    }

    private class ExponentialInstance implements Supplier<RetryDecision> {
      int attempt = 0;

      @Override
      public RetryDecision get() {
        return new RetryDecision(true, calculateBackoff());
      }

      private long calculateBackoff() {
        if (attempt < 0) {
          return max;
        }

        final int a = attempt++;
        final long candidate = (long) (base * Math.pow(factor, a));

        if (candidate <= max) {
          return candidate;
        }

        // indicate that max is reached.
        attempt = -1;
        return max;
      }
    }
  }

  /**
   * Builder of exponential retry policies.
   */
  class ExponentialBuilder {
    private final long base;

    ExponentialBuilder(final long base) {
      this.base = base;
    }

    private Optional<Long> max = Optional.empty();
    private Optional<Double> factor = Optional.empty();

    /**
     * Max possible delay.
     *
     * @param duration duration of max possible delay
     * @param unit unit of duration
     * @return this builder
     */
    public ExponentialBuilder max(final long duration, final TimeUnit unit) {
      this.max = Optional.of(ClockSource.UNIT.convert(duration, unit));
      return this;
    }

    /**
     * Factor to use when increasing the retry delay.
     *
     * @param factor number that must be greater than 1
     * @return this builder
     */
    public ExponentialBuilder factor(final double factor) {
      if (factor <= 1.0D) {
        throw new IllegalArgumentException("factor: must be greater than 1");
      }

      this.factor = Optional.of(factor);
      return this;
    }

    public Exponential build() {
      final double factor = this.factor.orElse(2D);
      final long max = this.max.orElse((long) (base * Math.pow(factor, 5D)));
      return new Exponential(base, factor, max);
    }
  }
}
