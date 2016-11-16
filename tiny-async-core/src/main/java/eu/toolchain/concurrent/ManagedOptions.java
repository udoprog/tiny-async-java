package eu.toolchain.concurrent;

import java.util.Optional;
import lombok.Data;

@Data
public class ManagedOptions {
  private static final boolean TRACING;
  private static final boolean CAPTURE_STACK;

  // fetch and compare the value of properties that modifies runtime behaviour of this class.
  static {
    TRACING = "on".equals(System.getProperty(Managed.TRACING, "off"));
    CAPTURE_STACK = "on".equals(System.getProperty(Managed.CAPTURE_STACK, "off"));
  }

  private final boolean tracing;
  private final boolean captureStack;

  public static Builder builder() {
    return new Builder();
  }

  public static ManagedOptions newDefault() {
    return builder().build();
  }

  public static class Builder {
    private Optional<Boolean> captureStack = Optional.empty();
    private Optional<Boolean> tracing = Optional.empty();

    Builder() {
    }

    /**
     * Configure if managed references should capture stack traces when borrowed.
     *
     * <p>This is mostly used for troubleshooting.
     *
     * @param captureStack {@code true} to capture stack traced
     * @return this builder
     */
    public Builder captureStack(final boolean captureStack) {
      this.captureStack = Optional.of(captureStack);
      return this;
    }

    /**
     * Configure if managed references should trace all it's borrowed references.
     *
     * <p>This is mostly used for troubleshooting.
     *
     * @param tracing {@code true} to trace borrowed references
     * @return this builder
     */
    public Builder tracing(final boolean tracing) {
      this.tracing = Optional.of(tracing);
      return this;
    }

    public ManagedOptions build() {
      final boolean captureStack = this.captureStack.orElse(CAPTURE_STACK);
      final boolean tracing = this.tracing.orElse(TRACING);
      return new ManagedOptions(captureStack, tracing);
    }
  }
}
