package eu.toolchain.async;

/**
 * Contains information about a retry that happened while trying to perform an operation
 */
public class RetryException extends RuntimeException {
    private final long offset;

    public RetryException(final long offset, final Throwable cause) {
        super(cause);
        this.offset = offset;
    }

    /**
     * Returns timing information for the retry. The value is an offset from a specific point in
     * time, usually from the beginning of the whole operation.
     *
     * @return Timing offset, in milliseconds
     */
    public long getOffsetMillis() {
        return offset;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + offset + "ms)";
    }
}
