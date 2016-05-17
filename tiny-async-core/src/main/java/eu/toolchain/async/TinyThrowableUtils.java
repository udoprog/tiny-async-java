package eu.toolchain.async;

import java.util.Collection;
import java.util.Iterator;

public class TinyThrowableUtils {
    public static Throwable buildCollectedException(Collection<Throwable> errors) {
        final Iterator<Throwable> it = errors.iterator();
        final Throwable first = it.next();

        while (it.hasNext()) {
            first.addSuppressed(it.next());
        }

        return first;
    }
}
