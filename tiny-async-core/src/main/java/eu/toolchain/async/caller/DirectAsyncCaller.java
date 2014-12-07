package eu.toolchain.async.caller;

import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.StreamCollector;

/**
 * An abstract implementation of a caller that invokes the handles directly in the calling thread.
 */
public abstract class DirectAsyncCaller implements AsyncCaller {
    @Override
    public <T> void resolveFutureDone(final FutureDone<T> handle, final T result) {
        try {
            handle.resolved(result);
        } catch (final Exception e) {
            internalError("FutureDone#resolve(T)", e);
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
            internalError("FutureDone#failed(Throwable)", e);
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
    public boolean isThreaded() {
        return false;
    }

    abstract protected void internalError(String what, Throwable e);
}
