package eu.toolchain.async;

import java.io.PrintStream;

import lombok.RequiredArgsConstructor;

/**
 * The simplest possible implementation of a concurrent caller.
 */
@RequiredArgsConstructor
public class PrintStreamDefaultAsyncCaller extends DirectAsyncCaller {
    public static final String CTX = PrintStreamDefaultAsyncCaller.class.getCanonicalName();

    private final PrintStream stream;

    @Override
    protected void internalError(String what, Throwable e) {
        stream.println(CTX + ": " + what);

        if (e != null)
            e.printStackTrace(stream);
    }
}