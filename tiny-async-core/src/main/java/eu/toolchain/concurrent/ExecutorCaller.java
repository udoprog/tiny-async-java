package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;

/**
 * A {@link Caller} implementation that defers all execution to a
 * {@link java.util.concurrent.ExecutorService}.
 */
@RequiredArgsConstructor
public final class ExecutorCaller implements Caller {
  private final ExecutorService executor;
  private final Caller caller;

  @Override
  public void execute(final Runnable runnable) {
    executor.execute(runnable);
  }

  @Override
  public void referenceLeaked(final Object reference, final StackTraceElement[] stack) {
    executor.execute(() -> caller.referenceLeaked(reference, stack));
  }
}
