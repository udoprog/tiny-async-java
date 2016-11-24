package eu.toolchain.concurrent;

/**
 * A value that can be completed at a later point in time.
 *
 * In general this follows the contract of {@link Stage}, but extends to to allow for the
 * completion of the underlying computation.
 *
 * @param <T> type of the value being completed
 * @see Stage
 */
public interface Completable<T> extends Stage<T> {
  /**
   * Complete the current stage.
   *
   * <p>This takes the stage out of the <em>pending</em> state and into the <em>completed</em>
   * state.
   *
   * @param result result of the computation
   * @return {@code true} if the completable was completed by this call
   */
  boolean complete(T result);

  /**
   * Fail the current stage.
   *
   * <p>This takes the stage out of the <em>pending</em> state and into the <em>cancelled</em>
   * state.
   *
   * @param cause the cause of the failure
   * @return {@code true} if the completable was completed by this call
   */
  boolean fail(Throwable cause);
}
