package eu.toolchain.async.caller;


/**
 * The simplest possible implementation of a concurrent caller.
 */
public class DefaultAsyncCaller extends DirectAsyncCaller {
    @Override
    protected void internalError(String what, Throwable e) {
        System.err.println(DefaultAsyncCaller.class.getCanonicalName() + ": " + what);
        e.printStackTrace(System.err);
    }
}