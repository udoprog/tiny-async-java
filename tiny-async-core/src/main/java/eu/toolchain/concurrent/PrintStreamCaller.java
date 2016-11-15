package eu.toolchain.concurrent;

import java.io.PrintStream;

/**
 * The simplest possible implementation of a concurrent caller.
 */
public class PrintStreamCaller extends DirectCaller {
  public static final String CTX = PrintStreamCaller.class.getCanonicalName();

  private final PrintStream stream;

  public PrintStreamCaller(final PrintStream stream) {
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
