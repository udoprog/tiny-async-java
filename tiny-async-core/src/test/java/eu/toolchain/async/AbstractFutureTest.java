package eu.toolchain.async;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Abstract tests that any Callback implementation have to pass.
 *
 * Use by extending in your own test class.
 *
 * @author udoprog
 * @param <T> Type of the callback implementation to test.
 */
public abstract class AbstractFutureTest<T extends ResolvableFuture<Object>> {
    private static final Object REFERENCE = new Object();
    private static final Exception ERROR = Mockito.mock(Exception.class);

    private AsyncCaller caller;

    @Getter
    private FutureDone<Object> handle;

    @Getter
    private T callback;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        caller = Mockito.mock(AsyncCaller.class);
        handle = Mockito.mock(FutureDone.class);
        callback = newCallback(caller);
    }

    abstract protected T newCallback(AsyncCaller caller);

    @Test
    public void testShouldFireCallbacks() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.on(handle);
        callback.resolve(REFERENCE);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(handle), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller).resolveFutureDone(handle, REFERENCE);

        callback.resolve(REFERENCE);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(handle), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller).resolveFutureDone(handle, REFERENCE);

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireCallbacksAfterResolve() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.resolve(REFERENCE);
        callback.on(handle);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(handle), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller).resolveFutureDone(handle, REFERENCE);

        // attempt a second register.
        callback.on(handle);

        // handle should have been called again.
        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(handle), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller, Mockito.atLeast(2)).resolveFutureDone(handle, REFERENCE);

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireFailure() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.on(handle);
        callback.fail(ERROR);

        Mockito.verify(caller).failFutureDone(handle, ERROR);
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller, Mockito.never()).resolveFutureDone(Mockito.eq(handle), Mockito.any());

        // attempt a second fail.
        callback.fail(ERROR);

        // should be same state.
        Mockito.verify(caller).failFutureDone(handle, ERROR);
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(handle));
        Mockito.verify(caller, Mockito.never()).resolveFutureDone(Mockito.eq(handle), Mockito.any());

        Assert.assertTrue(callback.isDone());
    }
}
