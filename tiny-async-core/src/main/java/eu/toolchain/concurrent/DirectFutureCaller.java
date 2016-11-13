package eu.toolchain.concurrent;

import static eu.toolchain.concurrent.CoreAsync.formatStack;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectFutureCaller implements FutureCaller {
  @Override
  public void execute(final Runnable runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      internalError("Failed to execute runnable", e);
    }
  }

  @Override
  public void referenceLeaked(Object reference, StackTraceElement[] stack) {
    internalError(String.format("reference %s leaked @ %s", reference, formatStack(stack)), null);
  }

  abstract protected void internalError(String what, Throwable e);
}
