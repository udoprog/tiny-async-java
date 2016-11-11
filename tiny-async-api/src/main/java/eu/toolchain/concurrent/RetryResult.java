package eu.toolchain.concurrent;

import java.util.List;

/**
 * Contains the result of a retry operation.
 * <p>
 * This class also carries any potential errors that were generated for prior requests {@link
 * #getErrors()}.
 *
 * @param <T> The type of the result.
 */
public class RetryResult<T> {
  final T result;
  final List<RetryException> errors;

  public RetryResult(final T result, final List<RetryException> errors) {
    this.result = result;
    this.errors = errors;
  }

  public T getResult() {
    return result;
  }

  public List<RetryException> getErrors() {
    return errors;
  }
}
