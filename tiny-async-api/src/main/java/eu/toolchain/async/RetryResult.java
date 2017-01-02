package eu.toolchain.async;

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
    final List<Long> backoffTimings;

    public RetryResult(
        final T result, final List<RetryException> errors, final List<Long> backoffTimings
    ) {
        this.result = result;
        this.errors = errors;
        this.backoffTimings = backoffTimings;
    }

    public T getResult() {
        return result;
    }

    /**
     * Returns information about the retries that happened prior to fulfilling this request.
     * <p>
     * Note that the timing 'offset' in each RetryException is the number of milliseconds since one
     * common point in time, the start of the whole operation.
     *
     * @return A list of RetryException's, where each one represents a failed try
     */
    public List<RetryException> getErrors() {
        return errors;
    }

    /**
     * Returns information about the retry backoff pauses that happened between retries (see {@link
     * #getErrors()}). Each failed retry is followed by a pause before trying another node, to avoid
     * hammering nodes in the case of an issue in the cluster.
     * <p>
     * Note that every timing in the list is the number of microseconds since one common point in
     * time, the start of the whole operation.
     *
     * @return A list of timings, in milliseconds
     */
    public List<Long> getBackoffTimings() {
        return backoffTimings;
    }
}
