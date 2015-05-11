package eu.toolchain.async;

public class ConcurrentResolvableFutureImmediateResolvedTest extends AbstractImmediateAsyncFuture {
    @Override
    protected AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result, Throwable cause) {
        final ConcurrentResolvableFuture<Object> future = new ConcurrentResolvableFuture<>(async, caller);
        future.resolve(result);
        return future;
    }

    @Override
    protected int setupResolved() {
        return 1;
    }
}