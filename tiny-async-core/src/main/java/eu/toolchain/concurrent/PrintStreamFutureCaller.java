package eu.toolchain.concurrent;

import java.io.PrintStream;

/**
 * The simplest possible implementation of a concurrent caller.
 */
public class PrintStreamFutureCaller extends DirectFutureCaller {
  public static final String CTX = PrintStreamFutureCaller.class.getCanonicalName();

  private final PrintStream stream;

  public PrintStreamFutureCaller(final PrintStream stream) {
    this.stream = stream;
  }

  @Override
  protected void internalError(String what, Throwable e) {
    stream.println(CTX + ": " + what);

    if (e != null) {
      e.printStackTrace(stream);
    }
  }
}
