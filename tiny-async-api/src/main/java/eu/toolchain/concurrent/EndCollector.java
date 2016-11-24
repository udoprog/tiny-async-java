package eu.toolchain.concurrent;

/**
 * A simplified collector that only cares about the end state of its children.
 *
 * @param <T> type returned by the collector
 */
@FunctionalInterface
public interface EndCollector<T> {
  /**
   * Apply the result of a collection of computation into a result.
   *
   * @param completed how many child computations ended in a <em>completed</em> state
   * @param failed how many child computations ended in a <em>failed</em> state
   * @param cancelled how many child computations ended in a <em>cancelled</em> state
   * @return the computed value
   */
  T apply(int completed, int failed, int cancelled);
}
