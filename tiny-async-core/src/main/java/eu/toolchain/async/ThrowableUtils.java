package eu.toolchain.async;

import java.util.Collection;

public class ThrowableUtils {
    public static String formatMultiMessage(Collection<Throwable> errors) {
        final StringBuilder builder = new StringBuilder();

        int i = 0;

        for (final Throwable e : errors) {
            builder.append(e.getMessage());

            if (++i < errors.size())
                builder.append(", ");
        }

        return builder.toString();
    }

    public static Throwable buildCollectedException(Collection<Throwable> errors) {
        final Exception e = new Exception(errors.size() + " exception(s) caught: "
                + ThrowableUtils.formatMultiMessage(errors));

        for (final Throwable s : errors)
            e.addSuppressed(s);

        return e;
    }
}
