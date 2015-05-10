package eu.toolchain.async;

import java.util.concurrent.ExecutorService;

import lombok.RequiredArgsConstructor;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.FutureCancelled;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.FutureFailed;
import eu.toolchain.async.FutureFinished;
import eu.toolchain.async.FutureResolved;
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
    public <T> void runFutureResolved(final FutureResolved<T> resolved, final T value) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.runFutureResolved(resolved, value);
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
    public void runFutureFailed(final FutureFailed failed, final Throwable cause) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.runFutureFailed(failed, cause);
            }
        });
    }

    @Override
    public <T> void leakedManagedReference(final T reference, final StackTraceElement[] stack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.leakedManagedReference(reference, stack);
            }
        });
    }

    @Override
    public boolean isThreaded() {
        return true;
    }
}