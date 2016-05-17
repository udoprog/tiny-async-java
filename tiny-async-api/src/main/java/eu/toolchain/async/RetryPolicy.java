package eu.toolchain.async;

/**
 * A policy that governs how and when an operation should be retried.
 * <p>
 * Policies decide if a retry should be performed or not by expiring after some given parameters.
 */
public interface RetryPolicy {
    /**
     * Apply the current policy.
     *
     * @return The decision arising from applying the current policy.
     */
    Instance apply(ClockSource clockSource);

    static RetryPolicy linear(long backoff) {
        return new Linear(backoff);
    }

    /**
     * Setup an exponential backoff retry policy.
     *
     * @param base The base time to backoff in milliseconds.
     * @return An exponential retry policy.
     */
    static RetryPolicy exponential(long base) {
        return exponential(base, (long) (base * Math.pow(2, 5)));
    }

    /**
     * Setup an exponential backoff retry policy.
     *
     * @param base The base time to backoff in milliseconds.
     * @param max The maximum allowed backoff in milliseconds.
     * @return An exponential retry policy.
     */
    static RetryPolicy exponential(long base, long max) {
        return new Exponential(base, max);
    }

    /**
     * Wrap an existing retry policy which is only valid for a given time.
     * <p>
     * This allows you to provide a custom clock source.
     *
     * @param duration The duration for which the policy should be valid.
     * @param policy The policy to wrap.
     * @return A timed retry policy.
     */
    static RetryPolicy timed(long duration, RetryPolicy policy) {
        return new Timed(duration, policy);
    }

    /**
     * A timed retry policy.
     */
    class Timed implements RetryPolicy {
        private final long duration;
        private final RetryPolicy policy;

        public Timed(final long duration, final RetryPolicy policy) {
            this.duration = duration;
            this.policy = policy;
        }

        @Override
        public Instance apply(ClockSource clockSource) {
            final long deadline = clockSource.now() + duration;
            final Instance inner = policy.apply(clockSource);

            return () -> {
                final Decision child = inner.next();
                final boolean shouldRetry = clockSource.now() < deadline && child.shouldRetry();
                return new Decision(shouldRetry, child.backoff());
            };
        }

        @Override
        public String toString() {
            return "Timed(duration=" + duration + ", policy=" + policy + ")";
        }
    }

    /**
     * A linear retry policy.
     */
    class Linear implements RetryPolicy {
        private final long backoff;

        public Linear(final long backoff) {
            this.backoff = backoff;
        }

        @Override
        public Instance apply(ClockSource clockSource) {
            final Decision decision = new Decision(true, backoff);
            return () -> decision;
        }

        @Override
        public String toString() {
            return "Linear(backoff=" + backoff + ")";
        }
    }

    /**
     * An exponential retry policy.
     */
    class Exponential implements RetryPolicy {
        private final long base;
        private final long max;

        public Exponential(final long base, final long max) {
            this.base = base;
            this.max = max;
        }

        @Override
        public Instance apply(ClockSource clockSource) {
            return new ExponentialInstance();
        }

        @Override
        public String toString() {
            return "Exponential(base=" + base + ", base=" + base + ")";
        }

        private class ExponentialInstance implements Instance {
            int attempt = 0;

            @Override
            public Decision next() {
                return new Decision(true, calculateBackoff());
            }

            private long calculateBackoff() {
                if (attempt < 0) {
                    return max;
                }

                int a = attempt++;
                final long candidate = (long) (base * Math.pow(2, a));

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
     * The decision of an applied retry policy.
     */
    class Decision {
        private final boolean shouldRetry;
        private final long backoff;

        public Decision(final boolean shouldRetry, final long backoff) {
            this.shouldRetry = shouldRetry;
            this.backoff = backoff;
        }

        /**
         * If another retry should be attemped.
         *
         * @return {@code true} if the operation should be retried, {@code false} othwerise.
         */
        public boolean shouldRetry() {
            return shouldRetry;
        }

        /**
         * How many milliseconds should the retry wait for until it can be retried.
         *
         * @return The number of milliseconds the retry should back off for.
         */
        public long backoff() {
            return backoff;
        }

        @Override
        public String toString() {
            return "Decision(shouldRetry=" + shouldRetry + ", backoff=" + backoff + ")";
        }
    }

    interface Instance {
        Decision next();
    }
}
