package eu.toolchain.async;

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;

/**
 * The simplest possible implementation of a concurrent caller.
 */
public class PrintStreamDefaultAsyncCaller extends DirectAsyncCaller {
    public static final String CTX = PrintStreamDefaultAsyncCaller.class.getCanonicalName();

    private final PrintStream stream;
    private final ExecutorService executor;

    public PrintStreamDefaultAsyncCaller(final PrintStream stream, final ExecutorService executor) {
        this.stream = stream;
        this.executor = executor;
    }

    @Override
    protected void internalError(String what, Throwable e) {
        stream.println(CTX + ": " + what);

        if (e != null) {
            e.printStackTrace(stream);
        }
    }

    @Override
    public void execute(final Runnable runnable) {
        executor.execute(runnable);
    }
}
