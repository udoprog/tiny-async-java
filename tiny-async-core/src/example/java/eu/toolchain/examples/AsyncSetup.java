package eu.toolchain.examples;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.TinyAsync;
import eu.toolchain.async.caller.DirectAsyncCaller;

public final class AsyncSetup {
    public static TinyAsync setup() {
        final TinyAsync.Builder builder = TinyAsync.builder();

        /* This step is optional, but recommended.
         *
         * It guarantees that all aspects of handle and task executions will happen on a thread pool.
         *
         * If a configuration is provided that would prevent this, an exception will be thrown att configuration time. */
        builder.threaded(true);

        /* This step is optional, but recommended.
         * 
         * If this is omitted, the default implementation will print these errors to stderr, and we don't like that.
         * 
         * Use a caller implementation that causes the invoking thread (direct) to execute handlers. This is relatively
         * sane, but can cause very deep stacks in certain situation. */
        final AsyncCaller caller = new DirectAsyncCaller() {
            /* This will be invoked on unexpected errors, use your logging framework to report them. */
            @Override
            public void internalError(String what, Throwable e) {
                System.out.println("Unexpected caller error: " + what);
                e.printStackTrace(System.out);
            }
        };

        builder.caller(caller);

        /* This step is optional, but recommended.
         * 
         * If the configuration of an Executor is omitted, then it will not be possible to call the corresponding
         * #resolve functions which requires a default Executor to be configured. */
        final ExecutorService executor = Executors.newFixedThreadPool(10);

        builder.executor(executor);

        return builder.build();
    }
}
