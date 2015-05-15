package eu.toolchain.async.immediate;

import eu.toolchain.async.AbstractImmediateAsyncFuture;
import eu.toolchain.async.ImmediateAsyncFutureTestBase;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.immediate.ImmediateFailedAsyncFuture;

public class ImmediateFailedAsyncFutureTest extends ImmediateAsyncFutureTestBase {
    @Override
    protected AbstractImmediateAsyncFuture<Object> setupFuture(AsyncFramework async, AsyncCaller caller, Object result,
            Throwable cause) {
        return new ImmediateFailedAsyncFuture<Object>(async, caller, cause);
    }

    @Override
    protected boolean setupFailed() {
        return true;
    }
}