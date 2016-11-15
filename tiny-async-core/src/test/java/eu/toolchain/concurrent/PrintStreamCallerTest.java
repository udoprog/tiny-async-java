package eu.toolchain.concurrent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;
import org.junit.Test;

public class PrintStreamCallerTest {
  private static final String message = "message";

  @Test
  public void testInternalError() {
    final PrintStream stream = mock(PrintStream.class);
    final Throwable e = mock(Throwable.class);

    final PrintStreamCaller caller = new PrintStreamCaller(stream);

    caller.internalError(message, e);

    verify(stream).println(PrintStreamCaller.CTX + ": " + message);
    verify(e).printStackTrace(stream);
  }

  @Test
  public void testInternalErrorNoThrowable() {
    final PrintStream stream = mock(PrintStream.class);

    final PrintStreamCaller caller = new PrintStreamCaller(stream);

    caller.internalError(message, null);

    verify(stream).println(PrintStreamCaller.CTX + ": " + message);
  }
}
