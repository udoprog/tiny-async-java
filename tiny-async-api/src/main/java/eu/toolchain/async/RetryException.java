package eu.toolchain.async;

public class RetryException extends RuntimeException {
    private final long offset;

    public RetryException(final long offset, final Throwable cause) {
        super(cause);
        this.offset = offset;
    }

    public long getOffsetMillis() {
        return offset;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + offset + "ms)";
    }
}
