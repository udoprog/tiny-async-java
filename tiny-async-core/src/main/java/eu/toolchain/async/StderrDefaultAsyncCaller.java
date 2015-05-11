package eu.toolchain.async;

/**
 * The simplest possible implementation of a concurrent caller.
 */
public class StderrDefaultAsyncCaller extends DirectAsyncCaller {
    private final String CTX = StderrDefaultAsyncCaller.class.getCanonicalName();

    @Override
    protected void internalError(String what, Throwable e) {
        System.err.println(CTX + ": " + what);
        e.printStackTrace(System.err);
    }
}