package eu.toolchain.examples;

import eu.toolchain.concurrent.CoreAsync;
import eu.toolchain.concurrent.DirectCaller;
import eu.toolchain.concurrent.Caller;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public final class Helpers {
  public static CoreAsync setup() {
    final CoreAsync.Builder builder = CoreAsync.builder();

    /* This step is optional, but recommended.
     *
     * It guarantees that all aspects of whenDone and task executions will happen on a thread
     * pool.
     *
     * If a configuration is provided that would prevent this, an exception will be thrown
     * att configuration time. */
    builder.threaded(true);

    /* This step is optional, but recommended.
     *
     * If this is omitted, the default implementation will print these errors to stderr, and
     * we don't like that.
     *
     * Use a caller implementation that causes the invoking thread (direct) to execute
     * handlers. This is relatively
     * sane, but can cause very deep stacks in certain situation. */
    final Caller caller = new DirectCaller() {
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
     * If the configuration of an Executor is omitted, then it will not be possible to doCall
     * the corresponding
     * #complete functions which requires a default Executor to be configured. */
    final ExecutorService executor = ForkJoinPool.commonPool();
    builder.executor(executor);

    return builder.build();
  }
}
