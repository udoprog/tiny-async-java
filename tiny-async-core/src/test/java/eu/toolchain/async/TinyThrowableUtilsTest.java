package eu.toolchain.async;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TinyThrowableUtilsTest {
    @Test
    public void testFormatMultiMessage() {
        final List<Throwable> errors = new ArrayList<>();

        errors.add(new Exception("foo"));
        errors.add(new Exception("bar"));
        errors.add(new Exception("baz"));

        final String message = TinyThrowableUtils.formatMultiMessage(errors);
        assertEquals("foo, bar, baz", message);
    }

    @Test
    public void testBuildCollectedException() {
        final List<Throwable> errors = new ArrayList<>();
        final Exception a = new Exception("foo");
        final Exception b = new Exception("bar");

        errors.add(a);
        errors.add(b);

        final Throwable e = TinyThrowableUtils.buildCollectedException(errors);
        assertEquals(a, e.getSuppressed()[0]);
        assertEquals(b, e.getSuppressed()[1]);
    }
}
