package eu.toolchain.concurrent;

import static eu.toolchain.concurrent.CoreAsync.formatStack;

import java.util.Arrays;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectCaller implements Caller {
  @Override
  public void execute(final Runnable runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      internalError("Failed to execute runnable", e);
    }
  }

  @Override
  public void referenceLeaked(final Object reference, final StackTraceElement[] stack) {
    final String s =
        stack.length > 0 ? "\n" + formatStack(Arrays.stream(stack), "  ") : " <unknown>";

    internalError(String.format("reference %s leaked at:%s", reference, s), null);
  }

  abstract protected void internalError(String what, Throwable e);
}
