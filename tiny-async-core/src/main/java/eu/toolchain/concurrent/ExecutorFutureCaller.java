package eu.toolchain.concurrent;

import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExecutorFutureCaller implements FutureCaller {
  private final ExecutorService executor;
  private final FutureCaller caller;

  @Override
  public void execute(final Runnable runnable) {
    executor.execute(runnable);
  }

  @Override
  public void referenceLeaked(final Object reference, final StackTraceElement[] stack) {
    executor.execute(() -> caller.referenceLeaked(reference, stack));
  }
}
