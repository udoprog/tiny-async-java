package eu.toolchain.async;

/**
 * A policy that governs how and when an operation should be retried.
 *
 * Policies decide if a retry should be performed or not by expiring after some given parameters.
 */
public interface RetryPolicy {
    /**
     * Apply the current policy.
     *
     * @return The decision arising from applying the current policy.
     */
    Decision apply();

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

    interface ClockSource {
        long now();

        ClockSource SYSTEM = new ClockSource() {
            @Override
            public long now() {
                return System.currentTimeMillis();
            }
        };

        static ClockSource system() {
            return SYSTEM;
        }
    }

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
     *
     * @param duration The duration in milliseconds for which the policy should be valid.
     * @param policy The policy to wrap.
     * @return A timed retry policy.
     */
    static RetryPolicy timed(long duration, RetryPolicy policy) {
        return timed(duration, policy, ClockSource.system());
    }

    /**
     * Wrap an existing retry policy which is only valid for a given time.
     * <p>
     * This allows you to provide a custom clock source.
     *
     * @param duration The duration for which the policy should be valid.
     * @param policy The policy to wrap.
     * @param clock The clock to use.
     * @return A timed retry policy.
     */
    static RetryPolicy timed(long duration, RetryPolicy policy, ClockSource clock) {
        final long deadline = clock.now() + duration;
        return new Timed(deadline, policy, clock);
    }

    /**
     * A timed retry policy.
     */
    class Timed implements RetryPolicy {
        private final long deadline;
        private final RetryPolicy policy;
        private final ClockSource clock;

        public Timed(
            final long deadline, final RetryPolicy policy, final ClockSource clock
        ) {
            this.deadline = deadline;
            this.policy = policy;
            this.clock = clock;
        }

        @Override
        public Decision apply() {
            final Decision child = policy.apply();
            final boolean shouldRetry = clock.now() < deadline && child.shouldRetry();
            return new Decision(shouldRetry, child.backoff());
        }

        @Override
        public String toString() {
            return "Timed(deadline=" + deadline + ", policy=" + policy + ", clock=" + clock + ")";
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
        public Decision apply() {
            return new Decision(true, backoff);
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
        int attempt = 0;

        private final long base;
        private final long max;

        public Exponential(final long base, final long max) {
            this.base = base;
            this.max = max;
        }

        @Override
        public Decision apply() {
            return new Decision(true, calculateBackoff());
        }

        private long calculateBackoff() {
            final long backoff;

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

        @Override
        public String toString() {
            return "Exponential(base=" + base + ", base=" + base + ")";
        }
    }
}
