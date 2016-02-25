package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;

public class ImmediateResolvedAsyncFutureTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<From> setupFuture(
        AsyncFramework async, AsyncCaller caller, From result, Throwable cause
    ) {
        return new ImmediateResolvedAsyncFuture<>(async, caller, result);
    }

    @Override
    protected ExpectedState setupState() {
        return ExpectedState.RESOLVED;
    }
}
