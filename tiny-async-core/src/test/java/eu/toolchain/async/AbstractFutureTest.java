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
@SuppressWarnings("unchecked")
public abstract class AbstractFutureTest<T extends ResolvableFuture<Object>> {
    private static final Object REFERENCE = new Object();
    private static final Exception ERROR = Mockito.mock(Exception.class);

    @Getter
    private AsyncFramework async;

    @Getter
    private AsyncCaller caller;

    @Getter
    private FutureDone<Object> futureDone;

    @Getter
    private FutureResolved<Object> futureResolved;

    @Getter
    private FutureFinished futureFinished;

    @Getter
    private FutureFailed futureFailed;

    @Getter
    private FutureCancelled futureCancelled;

    @Getter
    private T callback;

    @Before
    public void before() {
        async = Mockito.mock(AsyncFramework.class);
        caller = Mockito.mock(AsyncCaller.class);

        futureDone = Mockito.mock(FutureDone.class);
        futureResolved = Mockito.mock(FutureResolved.class);
        futureFinished = Mockito.mock(FutureFinished.class);
        futureFailed = Mockito.mock(FutureFailed.class);
        futureCancelled = Mockito.mock(FutureCancelled.class);

        callback = newCallback();
    }

    abstract protected T newCallback();

    @Test
    public void testShouldFireCallbacks() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.on(futureDone);
        callback.resolve(REFERENCE);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(futureDone), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller).resolveFutureDone(futureDone, REFERENCE);

        callback.resolve(REFERENCE);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(futureDone), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller).resolveFutureDone(futureDone, REFERENCE);

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireCallbacksAfterResolve() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.resolve(REFERENCE);
        callback.on(futureDone);

        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(futureDone), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller).resolveFutureDone(futureDone, REFERENCE);

        // attempt a second register.
        callback.on(futureDone);

        // handle should have been called again.
        Mockito.verify(caller, Mockito.never()).failFutureDone(Mockito.eq(futureDone), Mockito.any(Exception.class));
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller, Mockito.atLeast(2)).resolveFutureDone(futureDone, REFERENCE);

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireFailure() throws Exception {
        Assert.assertFalse(callback.isDone());

        callback.on(futureDone);
        callback.fail(ERROR);

        Mockito.verify(caller).failFutureDone(futureDone, ERROR);
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller, Mockito.never()).resolveFutureDone(Mockito.eq(futureDone), Mockito.any());

        // attempt a second fail.
        callback.fail(ERROR);

        // should be same state.
        Mockito.verify(caller).failFutureDone(futureDone, ERROR);
        Mockito.verify(caller, Mockito.never()).cancelFutureDone(Mockito.eq(futureDone));
        Mockito.verify(caller, Mockito.never()).resolveFutureDone(Mockito.eq(futureDone), Mockito.any());

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireResolved() throws Exception {
        Assert.assertFalse(callback.isDone());

        final FutureResolved<Object> resolved = Mockito.mock(FutureResolved.class);

        callback.on(resolved);
        Mockito.verify(caller, Mockito.never()).runFutureResolved(resolved, REFERENCE);

        callback.resolve(REFERENCE);
        Mockito.verify(caller, Mockito.only()).runFutureResolved(resolved, REFERENCE);

        callback.resolve(REFERENCE);
        Mockito.verify(caller, Mockito.only()).runFutureResolved(resolved, REFERENCE);

        callback.on(resolved);
        Mockito.verify(caller, Mockito.times(2)).runFutureResolved(resolved, REFERENCE);

        Assert.assertTrue(callback.isDone());
    }

    @Test
    public void testShouldFireMixedCallbacks() {

    }
}
