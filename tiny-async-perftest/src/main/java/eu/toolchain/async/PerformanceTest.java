package eu.toolchain.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceTest {
    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    private static final Runtime runtime = Runtime.getRuntime();

    public static void main(String argv[]) throws IOException {
        System.in.read();

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
        final List<Long> b = runTest(tiny);
        final List<Long> a = b; // runTest(guava);

        System.out.println(name);
        System.out.println("  avg: " + time(avg(a)) + " - " + time(avg(b)));
        System.out.println("  p50: " + time(q(a, 0.5)) + " - " + time(q(b, 0.5)));
        System.out.println("  p95: " + time(q(a, 0.95)) + " - " + time(q(b, 0.95)));
        System.out.println("  p99: " + time(q(a, 0.99)) + " - " + time(q(b, 0.99)));
        System.out.println("  " + Math.round(((double) avg(a) / (double) avg(b)) * 100d) + "%");
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
