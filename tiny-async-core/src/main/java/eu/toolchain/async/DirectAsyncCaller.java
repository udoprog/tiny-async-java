package eu.toolchain.async;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectAsyncCaller implements AsyncCaller {
    @Override
    public <T> void resolve(final FutureDone<T> handle, final T result) {
        try {
            handle.resolved(result);
        } catch (final Exception e) {
            internalError("FutureDone#resolved(T)", e);
        }
    }

    @Override
    public <T> void fail(FutureDone<T> handle, Throwable error) {
        try {
            handle.failed(error);
        } catch (final Exception e) {
            internalError("FutureDone#failed(Throwable)", e);
        }
    }

    @Override
    public <T> void cancel(FutureDone<T> handle) {
        try {
            handle.cancelled();
        } catch (final Exception e) {
            internalError("FutureDone#cancelled()", e);
        }
    }

    @Override
    public void finish(FutureFinished finishable) {
        try {
            finishable.finished();
        } catch (final Exception e) {
            internalError("FutureFinished#finished()", e);
        }
    }

    @Override
    public void cancel(FutureCancelled cancelled) {
        try {
            cancelled.cancelled();
        } catch (final Exception e) {
            internalError("FutureCancelled#cancelled()", e);
        }
    }

    @Override
    public <T> void resolve(FutureResolved<T> resolved, T value) {
        try {
            resolved.resolved(value);
        } catch (final Exception e) {
            internalError("FutureResolved#resolved(T)", e);
        }
    }

    @Override
    public void fail(FutureFailed failed, Throwable cause) {
        try {
            failed.failed(cause);
        } catch (final Exception e) {
            internalError("FutureFailed#failed(Throwable)", e);
        }
    }

    @Override
    public <S, T> void resolve(StreamCollector<S, T> collector, S result) {
        try {
            collector.resolved(result);
        } catch (final Exception e) {
            internalError("StreamCollector#resolved(T)", e);
        }
    }

    @Override
    public <S, T> void fail(StreamCollector<S, T> collector, Throwable error) {
        try {
            collector.failed(error);
        } catch (final Exception e) {
            internalError("StreamCollector#failed(Throwable)", e);
        }
    }

    @Override
    public <S, T> void cancel(StreamCollector<S, T> collector) {
        try {
            collector.cancelled();
        } catch (final Exception e) {
            internalError("StreamCollector#cancel()", e);
        }
    }

    @Override
    public <T> void referenceLeaked(T reference, StackTraceElement[] stack) {
        internalError(String.format("reference %s leaked @ %s", reference, formatStack(stack)), null);
    }

    @Override
    public boolean isThreaded() {
        return false;
    }

    protected String formatStack(StackTraceElement[] stack) {
        return TinyStackUtils.formatStack(stack);
    }

    abstract protected void internalError(String what, Throwable e);
}
