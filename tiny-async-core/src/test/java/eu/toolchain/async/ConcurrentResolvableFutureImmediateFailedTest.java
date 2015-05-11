package eu.toolchain.async;

public class ConcurrentResolvableFutureImmediateFailedTest extends AbstractImmediateAsyncFuture {
    @Override
    protected AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result, Throwable cause) {
        final ConcurrentResolvableFuture<Object> future = new ConcurrentResolvableFuture<>(async, caller);
        future.fail(cause);
        return future;
    }

    @Override
    protected int setupFailed() {
        return 1;
    }
}