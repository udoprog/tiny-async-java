package eu.toolchain.async;

public class ConcurrentResolvableFutureImmediateCancelledTest extends AbstractImmediateAsyncFuture {
    @Override
    protected AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result, Throwable cause) {
        final ConcurrentResolvableFuture<Object> future = new ConcurrentResolvableFuture<>(async, caller);
        future.cancel();
        return future;
    }

    @Override
    protected int setupCancelled() {
        return 1;
    }
}