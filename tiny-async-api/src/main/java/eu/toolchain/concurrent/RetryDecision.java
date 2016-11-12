package eu.toolchain.concurrent;

/**
 * The decision of an applied retry policy.
 *
 * @see eu.toolchain.concurrent.RetryPolicy
 */
public class RetryDecision {
  private final boolean shouldRetry;
  private final long backoff;

  RetryDecision(final boolean shouldRetry, final long backoff) {
    this.shouldRetry = shouldRetry;
    this.backoff = backoff;
  }

  /**
   * If another retry should be attemped.
   *
   * @return {@code true} if the operation should be retried.
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
    return "RetryDecision(shouldRetry=" + shouldRetry + ", backoff=" + backoff + ")";
  }
}
