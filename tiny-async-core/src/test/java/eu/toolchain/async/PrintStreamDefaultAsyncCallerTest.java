package eu.toolchain.async;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;

import org.junit.Test;

public class PrintStreamDefaultAsyncCallerTest {
    private static final String message = "message";

    @Test
    public void testInternalError() {
        final PrintStream stream = mock(PrintStream.class);
        final Throwable e = mock(Throwable.class);

        final PrintStreamDefaultAsyncCaller caller = new PrintStreamDefaultAsyncCaller(stream);

        caller.internalError(message, e);

        verify(stream).println(PrintStreamDefaultAsyncCaller.CTX + ": " + message);
        verify(e).printStackTrace(stream);
    }

    @Test
    public void testInternalErrorNoThrowable() {
        final PrintStream stream = mock(PrintStream.class);

        final PrintStreamDefaultAsyncCaller caller = new PrintStreamDefaultAsyncCaller(stream);

        caller.internalError(message, null);

        verify(stream).println(PrintStreamDefaultAsyncCaller.CTX + ": " + message);
    }
}