package eu.toolchain.async;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TinyThrowableUtilsTest {
    @Test
    public void testBuildCollectedException() {
        final List<Throwable> errors = new ArrayList<>();
        final Exception a = new Exception("foo");
        final Exception b = new Exception("bar");

        errors.add(a);
        errors.add(b);

        final Throwable e = TinyThrowableUtils.buildCollectedException(errors);
        assertEquals(b, e.getSuppressed()[0]);
    }
}
