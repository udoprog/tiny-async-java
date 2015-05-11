package eu.toolchain.async;

public class FailedAsyncFutureTest extends AbstractImmediateAsyncFuture {
    @Override
    protected int setupFailed() {
        return 1;
    }

    @Override
    protected AsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result, Throwable cause) {
        return new FailedAsyncFuture<Object>(async, caller, cause);
    }
}