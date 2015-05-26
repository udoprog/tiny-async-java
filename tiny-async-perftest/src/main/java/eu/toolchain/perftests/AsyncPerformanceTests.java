package eu.toolchain.perftests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * These tests are only in place to verify to a loose degree that we get at least as good performance as Guava.
 *
 * @author udoprog
 */
public class AsyncPerformanceTests {
    public static void main(String argv[]) throws RunnerException {
        final ChainedOptionsBuilder builder = new OptionsBuilder().include("eu\\.toolchain\\.perftests.jmh").forks(1);
        final Collection<RunResult> results = new Runner(builder.build()).run();

        // graph(results);
    }

    private static void graph(final Collection<RunResult> results) {
        final XYSeriesCollection data = new XYSeriesCollection();

        for (final RunResult result : results) {
            final BenchmarkResult a = result.getAggregatedResult();

            int index = 0;

            final XYSeries series = new XYSeries(result.getParams().getBenchmark());

            for (IterationResult iteration : a.getIterationResults()) {
                series.add(index++, iteration.getPrimaryResult().getScore());
            }

            result.getPrimaryResult().getScoreUnit();
            data.addSeries(series);
        }

        final List<Charts.Dataset> datasets = new ArrayList<>();
        datasets.add(new Charts.Dataset("iteration", "ops/s", data));
        Charts.showChart("benchmarks", Charts.createChart("benchmarks", datasets));
    }
}
