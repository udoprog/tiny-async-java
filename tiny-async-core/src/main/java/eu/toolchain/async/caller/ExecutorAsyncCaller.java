package eu.toolchain.async.caller;

import java.util.concurrent.ExecutorService;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.StreamCollector;

@RequiredArgsConstructor
public final class ExecutorAsyncCaller implements AsyncCaller {
    private final ExecutorService executor;
    private final AsyncCaller caller;

    @Override
    public <T> void resolveFutureDone(final FutureDone<T> handle, final T result) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.resolveFutureDone(handle, result);
            }
        });
    }

    @Override
    public <T> void failFutureDone(final FutureDone<T> handle, final Throwable error) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.failFutureDone(handle, error);
            }
        });
    }

    @Override
    public <T> void cancelFutureDone(final FutureDone<T> handle) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.cancelFutureDone(handle);
            }
        });
    }

    @Override
    public void runFutureCancelled(final FutureCancelled cancelled) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.runFutureCancelled(cancelled);
            }
        });
    }

    @Override
    public void runFutureFinished(final FutureFinished finishable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.runFutureFinished(finishable);
            }
        });
    }

    @Override
    public <T, R> void resolveStreamCollector(final StreamCollector<T, R> collector, final T result) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.resolveStreamCollector(collector, result);
            }
        });
    }

    @Override
    public <T, R> void failStreamCollector(final StreamCollector<T, R> collector, final Throwable error) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.failStreamCollector(collector, error);
            }
        });
    }

    @Override
    public <T, R> void cancelStreamCollector(final StreamCollector<T, R> collector) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.cancelStreamCollector(collector);
            }
        });
    }

    @Override
    public boolean isThreaded() {
        return true;
    }
}