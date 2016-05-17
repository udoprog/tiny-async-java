package eu.toolchain.async;

public class RetryException extends RuntimeException {
    private final long when;

    public RetryException(final long when, final Throwable cause) {
        super(cause);
        this.when = when;
    }

    public long getWhen() {
        return when;
    }
}
