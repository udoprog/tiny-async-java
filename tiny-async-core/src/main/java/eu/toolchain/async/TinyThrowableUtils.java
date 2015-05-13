package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;

public class TinyThrowableUtils {
    public static String formatMultiMessage(Collection<Throwable> errors) {
        final StringBuilder builder = new StringBuilder();

        final Iterator<Throwable> iter = errors.iterator();

        while (iter.hasNext()) {
            builder.append(iter.next().getMessage());

            if (iter.hasNext())
                builder.append(", ");
        }

        return builder.toString();
    }

    public static Throwable buildCollectedException(Collection<Throwable> errors) {
        final Exception e = new Exception(errors.size() + " exception(s) caught: " + formatMultiMessage(errors));

        for (final Throwable s : errors)
            e.addSuppressed(s);

        return e;
    }
}
