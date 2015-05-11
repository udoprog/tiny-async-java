package eu.toolchain.async;

public class CancelledAsyncFutureTest extends AbstractImmediateAsyncFuture {
    @Override
    protected int setupCancelled() {
        return 1;
    }

    @Override
    protected AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result, Throwable cause) {
        return new CancelledAsyncFuture<Object>(async, caller);
    }
}