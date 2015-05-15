package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.immediate.ImmediateCancelledAsyncFuture;

public class ImmediateCancelledAsyncFutureTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result,
            Throwable cause) {
        return new ImmediateCancelledAsyncFuture<Object>(async, caller);
    }

    @Override
    protected boolean setupCancelled() {
        return true;
    }
}