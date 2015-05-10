package eu.toolchain.async.caller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
import eu.toolchain.async.StreamCollector;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectAsyncCaller implements AsyncCaller {
    private static final String STACK_LINE_FORMAT = "%s.%s (%s:%d)";

    @Override
    public <T> void resolveFutureDone(final FutureDone<T> handle, final T result) {
        try {
            handle.resolved(result);
        } catch (final Exception e) {
            internalError("FutureDone#resolved(T)", e);
        }
    }

    @Override
    public <T> void failFutureDone(FutureDone<T> handle, Throwable error) {
        try {
            handle.failed(error);
        } catch (final Exception e) {
            internalError("FutureDone#failed(Throwable)", e);
        }
    }

    @Override
    public <T> void cancelFutureDone(FutureDone<T> handle) {
        try {
            handle.cancelled();
        } catch (final Exception e) {
            internalError("FutureDone#cancelled()", e);
        }
    }

    @Override
    public void runFutureFinished(FutureFinished finishable) {
        try {
            finishable.finished();
        } catch (final Exception e) {
            internalError("FutureFinished#finished()", e);
        }
    }

    @Override
    public void runFutureCancelled(FutureCancelled cancelled) {
        try {
            cancelled.cancelled();
        } catch (final Exception e) {
            internalError("FutureCancelled#cancelled()", e);
        }
    }

    @Override
    public <T> void runFutureResolved(FutureResolved<T> resolved, T value) {
        try {
            resolved.resolved(value);
        } catch (final Exception e) {
            internalError("FutureResolved#resolved(T)", e);
        }
    }

    @Override
    public void runFutureFailed(FutureFailed failed, Throwable cause) {
        try {
            failed.failed(cause);
        } catch (final Exception e) {
            internalError("FutureFailed#failed(Throwable)", e);
        }
    }

    @Override
    public <T, R> void resolveStreamCollector(StreamCollector<T, R> collector, T result) {
        try {
            collector.resolved(result);
        } catch (final Exception e) {
            internalError("StreamCollector#resolved(T)", e);
        }
    }

    @Override
    public <T, R> void failStreamCollector(StreamCollector<T, R> collector, Throwable error) {
        try {
            collector.failed(error);
        } catch (final Exception e) {
            internalError("StreamCollector#failed(Throwable)", e);
        }
    }

    @Override
    public <T, R> void cancelStreamCollector(StreamCollector<T, R> collector) {
        try {
            collector.cancelled();
        } catch (final Exception e) {
            internalError("StreamCollector#cancel()", e);
        }
    }

    @Override
    public <T> void leakedManagedReference(T reference, StackTraceElement[] stack) {
        internalError(String.format("reference %s leaked @ %s", reference, formatStack(stack)), null);
    }

    @Override
    public boolean isThreaded() {
        return false;
    }

    private static String formatStack(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0)
            return "unknown";

        final List<String> entries = new ArrayList<>(stack.length);

        for (final StackTraceElement e : stack) {
            entries.add(String.format(STACK_LINE_FORMAT, e.getClassName(), e.getMethodName(), e.getFileName(),
                    e.getLineNumber()));
        }

        final Iterator<String> it = entries.iterator();

        final StringBuilder builder = new StringBuilder();

        while (it.hasNext()) {
            builder.append(it.next());

            if (it.hasNext()) {
                builder.append("\n  ");
            }
        }

        return builder.toString();
    }

    abstract protected void internalError(String what, Throwable e);
}
