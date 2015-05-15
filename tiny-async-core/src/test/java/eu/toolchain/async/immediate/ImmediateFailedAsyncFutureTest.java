package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;

public class ImmediateFailedAsyncFutureTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<From> setupFuture(AsyncFramework async, AsyncCaller caller, From result,
            Throwable cause) {
        return new ImmediateFailedAsyncFuture<>(async, caller, cause);
    }

    @Override
    protected ExpectedState setupState() {
        return ExpectedState.FAILED;
    }
}