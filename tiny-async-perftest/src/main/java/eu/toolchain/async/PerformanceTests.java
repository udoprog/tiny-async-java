package eu.toolchain.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.toolchain.async.perftests.Immediate;
import eu.toolchain.async.perftests.ManyListeners;
import eu.toolchain.async.perftests.ManyThreads;
import eu.toolchain.async.perftests.TransformFew;
import eu.toolchain.async.perftests.TransformMany;

/**
 * These tests are only in place to verify to a loose degree that we get at least as good performance as Guava.
 *
 * @author udoprog
 */
public class PerformanceTests {
    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    private static final Runtime runtime = Runtime.getRuntime();

    public static void main(String argv[]) {
        run("many listeners", new ManyListeners.Guava(), new ManyListeners.Tiny());

        run("immediate", new Immediate.Guava(), new Immediate.Tiny());

        run("immediate, into many transforms", new TransformMany.Guava(), new TransformMany.Tiny());

        run("immediate, into few transforms", new TransformFew.Guava(), new TransformFew.Tiny());

        run("many threads contending", new ManyThreads.Guava(), new ManyThreads.Tiny());
    }

    private static List<Long> runTest(TestCase test) {
        runtime.gc();

        final List<Long> samples = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < WARMUP; i++) {
            try {
                test.test();
            } catch (Exception e) {
            }
        }

        final List<Exception> errors = new ArrayList<>();

        for (int i = 0; i < ITERATIONS; i++) {
            final long then = System.nanoTime();

            try {
                test.test();
            } catch (Exception e) {
                errors.add(e);
            }

            final long diff = System.nanoTime() - then;

            samples.add(diff);
        }

        if (!errors.isEmpty()) {
            for (int i = 0; i < errors.size(); i++) {
                final Exception e = errors.get(i);
                System.out.println("Error #" + i + ": " + e.getMessage());
                e.printStackTrace(System.out);
            }

            throw new IllegalStateException("test threw errors");
        }

        Collections.sort(samples);
        return samples;
    }

    private static void run(String name, TestCase guava, TestCase tiny) {
        // hint that we want a clean state :).
        final List<Long> g = runTest(guava);
        final List<Long> t = runTest(tiny);

        System.out.println(name + " (guava - tiny)");
        System.out.println("  avg: " + time(avg(g)) + " - " + time(avg(t)));
        System.out.println("  p50: " + time(q(g, 0.5)) + " - " + time(q(t, 0.5)));
        System.out.println("  p95: " + time(q(g, 0.95)) + " - " + time(q(t, 0.95)));
        System.out.println("  p99: " + time(q(g, 0.99)) + " - " + time(q(t, 0.99)));
        System.out.println("  " + Math.round(((double) avg(g) / (double) avg(t)) * 100d) + "%");
    }

    private static long avg(List<Long> samples) {
        long total = 0;

        for (long sample : samples)
            total += sample;

        return total / samples.size();
    }

    private static long q(List<Long> samples, double q) {
        final int target = Math.min((int) Math.round(samples.size() * q), samples.size() - 1);
        return samples.get(target);
    }

    private static String time(long ns) {
        if (ns > 1000000)
            return (Math.round((ns / 1000000d)) / 1000d) + "s";

        if (ns > 1000)
            return (Math.round((ns / 1000d)) / 1000d) + "ms";

        return (ns / 1000d) + "μs";
    }
}
