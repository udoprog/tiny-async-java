package eu.toolchain.perftests;

import java.util.Collection;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * These tests are only in place to verify to a loose degree that we get at least as good performance as Guava.
 *
 * @author udoprog
 */
public class AsyncPerformanceTests {
    public static void main(String argv[]) throws Exception {
        final ChainedOptionsBuilder builder = new OptionsBuilder().include("eu\\.toolchain\\.perftests\\.jmh").forks(1)
                .warmupIterations(4).measurementIterations(4);
        final Collection<RunResult> results = new Runner(builder.build()).run();
    }
}
