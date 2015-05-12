package eu.toolchain.perftests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

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
        run("many listeners", new ManyListeners());

        run("immediate", new Immediate());

        run("immediate, into many transforms", new TransformMany());

        run("immediate, into few transforms", new TransformFew());

        run("many threads contending", new ManyThreads());
    }

    private static List<Long> runTest(Callable<Void> runnable) {
        runtime.gc();

        final List<Long> samples = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < WARMUP; i++) {
            try {
                runnable.call();
            } catch (Exception e) {
            }
        }

        final List<Exception> errors = new ArrayList<>();

        for (int i = 0; i < ITERATIONS; i++) {
            final long then = System.nanoTime();

            try {
                runnable.call();
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

    private static void run(String name, final TestCase test) {
        // hint that we want a clean state :).
        final List<Long> g = runTest(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                test.guava();
                return null;
            }
        });

        final List<Long> t = runTest(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                test.tiny();
                return null;
            }
        });

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
