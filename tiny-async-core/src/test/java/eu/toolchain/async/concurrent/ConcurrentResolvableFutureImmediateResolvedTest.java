package eu.toolchain.async.concurrent;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.concurrent.ConcurrentResolvableFuture;

public class ConcurrentResolvableFutureImmediateResolvedTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result,
            Throwable cause) {
        final ConcurrentResolvableFuture<Object> future = new ConcurrentResolvableFuture<>(async, caller);
        future.resolve(result);
        return future;
    }

    @Override
    protected boolean setupResolved() {
        return true;
    }
}