package eu.toolchain.async.concurrent;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;

public class ConcurrentResolvableFutureImmediateResolvedTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<From> setupFuture(AsyncFramework async, AsyncCaller caller, From result,
            Throwable cause) {
        final ConcurrentResolvableFuture<From> future = new ConcurrentResolvableFuture<>(async, caller);
        future.resolve(result);
        return future;
    }

    @Override
    protected ExpectedState setupState() {
        return ExpectedState.RESOLVED;
    }
}