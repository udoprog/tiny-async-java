package eu.toolchain.concurrent;

/**
 * An exception class containing detailed information about when a retried operation failed.
 */
public class RetryException extends RuntimeException {
  private final long offset;

  public RetryException(final long offset, final Throwable cause) {
    super(cause);
    this.offset = offset;
  }

  /**
   * Offset from start time in milliseconds that this failure happened.
   *
   * @return milliseconds
   */
  public long getOffsetMillis() {
    return offset;
  }

  @Override
  public String toString() {
    return super.toString() + " (" + offset + "ms)";
  }
}
