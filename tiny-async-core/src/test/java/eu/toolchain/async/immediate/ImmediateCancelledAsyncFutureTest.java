package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;

public class ImmediateCancelledAsyncFutureTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<From> setupFuture(AsyncFramework async, AsyncCaller caller, From result,
            Throwable cause) {
        return new ImmediateCancelledAsyncFuture<From>(async, caller);
    }

    @Override
    protected ExpectedState setupState() {
        return ExpectedState.CANCELLED;
    }
}