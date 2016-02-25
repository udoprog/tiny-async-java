package eu.toolchain.async;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public final class ExecutorAsyncCaller implements AsyncCaller {
    private final ExecutorService executor;
    private final AsyncCaller caller;

    @Override
    public <T> void resolve(final FutureDone<T> handle, final T result) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.resolve(handle, result);
            }
        });
    }

    @Override
    public <T> void fail(final FutureDone<T> handle, final Throwable error) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.fail(handle, error);
            }
        });
    }

    @Override
    public <T> void cancel(final FutureDone<T> handle) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.cancel(handle);
            }
        });
    }

    @Override
    public void cancel(final FutureCancelled cancelled) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.cancel(cancelled);
            }
        });
    }

    @Override
    public void finish(final FutureFinished finishable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.finish(finishable);
            }
        });
    }

    @Override
    public <T> void resolve(final FutureResolved<T> resolved, final T value) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.resolve(resolved, value);
            }
        });
    }

    @Override
    public <T, R> void resolve(final StreamCollector<T, R> collector, final T result) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.resolve(collector, result);
            }
        });
    }

    @Override
    public <T, R> void fail(final StreamCollector<T, R> collector, final Throwable error) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.fail(collector, error);
            }
        });
    }

    @Override
    public <T, R> void cancel(final StreamCollector<T, R> collector) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.cancel(collector);
            }
        });
    }

    @Override
    public void fail(final FutureFailed failed, final Throwable cause) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.fail(failed, cause);
            }
        });
    }

    @Override
    public <T> void referenceLeaked(final T reference, final StackTraceElement[] stack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                caller.referenceLeaked(reference, stack);
            }
        });
    }

    @Override
    public boolean isThreaded() {
        return true;
    }
}
