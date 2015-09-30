package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

@RunWith(MockitoJUnitRunner.class)
public abstract class ImmediateAsyncFutureTestBase {
    @Mock
    private Throwable cause;
    @Mock
    private From result;
    @Mock
    private To to;
    @Mock
    private AsyncFramework async;
    @Mock
    private AsyncCaller caller;
    @Mock
    private FutureDone<From> done;
    @Mock
    private FutureResolved<From> resolved;
    @Mock
    private FutureFinished finished;
    @Mock
    private FutureFailed failed;
    @Mock
    private FutureCancelled cancelled;
    @Mock
    private AsyncFuture<?> other;

    private AbstractImmediateAsyncFuture<From> underTest;

    private ExpectedState expected;

    protected abstract AbstractImmediateAsyncFuture<From> setupFuture(AsyncFramework async, AsyncCaller caller,
            From result, Throwable cause);

    protected abstract ExpectedState setupState();

    @Rule
    public ExpectedException except = ExpectedException.none();

    @Before
    public void setup() {
        underTest = spy(setupFuture(async, caller, result, cause));
        expected = setupState();
    }

    @Test
    public void testCancel() {
        assertFalse(underTest.cancel());
    }

    @Test
    public void testCancelWithParameter() {
        assertFalse(underTest.cancel(true));
        assertFalse(underTest.cancel(false));
    }

    @Test
    public void testOnFutureDone() throws Exception {
        underTest.on(done);
        verify(caller, cancelled()).cancel(done);
        verify(caller, resolved()).resolve(done, result);
        verify(caller, failed()).fail(done, cause);
    }

    @Test
    public void testOnAnyFuturedone() throws Exception {
        underTest.onAny(done);
        verify(caller, cancelled()).cancel(done);
        verify(caller, resolved()).resolve(done, result);
        verify(caller, failed()).fail(done, cause);
    }

    @Test
    public void testBind() throws Exception {
        underTest.bind(other);
        verify(other, cancelled()).cancel();
    }

    @Test
    public void testOnFutureFinished() throws Exception {
        underTest.on(finished);
        verify(caller).finish(finished);
    }

    @Test
    public void testOnFutureFailed() throws Exception {
        underTest.on(failed);
        verify(caller, failed()).fail(failed, cause);
    }

    @Test
    public void testOnFutureCancelled() throws Exception {
        underTest.on(cancelled);
        verify(caller, cancelled()).cancel(cancelled);
    }

    @Test
    public void testOnFutureResolved() throws Exception {
        underTest.on(resolved);
        verify(caller, resolved()).resolve(resolved, result);
    }

    @Test
    public void testIsDone() throws Exception {
        assertTrue(underTest.isDone());
    }

    @Test
    public void testIsResolved() throws Exception {
        assertEquals(isResolved(), underTest.isResolved());
    }

    @Test
    public void testIsFailed() throws Exception {
        assertEquals(isFailed(), underTest.isFailed());
    }

    @Test
    public void testIsCancelled() throws Exception {
        assertEquals(isCancelled(), underTest.isCancelled());
    }

    @Test
    public void testCause() throws Exception {
        if (!isFailed())
            except.expect(IllegalStateException.class);

        assertNotNull(underTest.cause());
    }

    @Test
    public void testGet() throws Exception {
        if (isCancelled())
            except.expect(CancellationException.class);

        if (isFailed())
            except.expect(ExecutionException.class);

        underTest.get();
    }

    @Test
    public void testGetWithTimeout() throws Exception {
        if (isCancelled())
            except.expect(CancellationException.class);

        if (isFailed())
            except.expect(ExecutionException.class);

        underTest.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testGetNow() throws Exception {
        if (isCancelled())
            except.expect(CancellationException.class);

        if (isFailed())
            except.expect(ExecutionException.class);

        underTest.getNow();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransform() throws Exception {
        final Transform<From, To> transform = mock(Transform.class);
        final AsyncFuture<To> target = mock(AsyncFuture.class);

        doReturn(target).when(underTest).transformResolved(transform, result);
        doReturn(target).when(async).cancelled();
        doReturn(target).when(async).failed(any(TransformException.class));

        assertEquals(target, underTest.transform(transform));

        verify(underTest, resolved()).transformResolved(transform, result);
        verify(async, cancelled()).cancelled();
        verify(async, failed()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransform() throws Exception {
        final LazyTransform<From, To> transform = mock(LazyTransform.class);
        final AsyncFuture<To> target = mock(AsyncFuture.class);

        doReturn(target).when(underTest).lazyTransformResolved(transform, result);
        doReturn(target).when(async).cancelled();
        doReturn(target).when(async).failed(any(TransformException.class));

        assertEquals(target, underTest.lazyTransform(transform));

        verify(underTest, resolved()).lazyTransformResolved(transform, result);
        verify(async, cancelled()).cancelled();
        verify(async, failed()).failed(any(TransformException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransformFailed() throws Exception {
        final Transform<Throwable, From> transform = mock(Transform.class);

        doReturn(underTest).when(underTest).transformFailed(transform, cause);

        assertEquals(underTest, underTest.catchFailed(transform));

        verify(underTest, failed()).transformFailed(transform, cause);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransformFailed() throws Exception {
        final LazyTransform<Throwable, From> transform = mock(LazyTransform.class);

        doReturn(underTest).when(underTest).lazyTransformFailed(transform, cause);

        assertEquals(underTest, underTest.lazyCatchFailed(transform));

        verify(underTest, failed()).lazyTransformFailed(transform, cause);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransformCancelled() throws Exception {
        final Transform<Void, From> transform = mock(Transform.class);

        doReturn(underTest).when(underTest).transformCancelled(transform);

        assertEquals(underTest, underTest.catchCancelled(transform));

        verify(underTest, cancelled()).transformCancelled(transform);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransformCancelled() throws Exception {
        final LazyTransform<Void, From> transform = mock(LazyTransform.class);

        doReturn(underTest).when(underTest).lazyTransformCancelled(transform);

        assertEquals(underTest, underTest.lazyCatchCancelled(transform));

        verify(underTest, cancelled()).lazyTransformCancelled(transform);
    }

    private boolean isResolved() {
        return expected == ExpectedState.RESOLVED;
    }

    private boolean isCancelled() {
        return expected == ExpectedState.CANCELLED;
    }

    private boolean isFailed() {
        return expected == ExpectedState.FAILED;
    }

    private VerificationMode resolved() {
        if (expected == ExpectedState.RESOLVED)
            return times(1);

        return never();
    }

    private VerificationMode cancelled() {
        if (expected == ExpectedState.CANCELLED)
            return times(1);

        return never();
    }

    private VerificationMode failed() {
        if (expected == ExpectedState.FAILED)
            return times(1);

        return never();
    }

    protected static interface From {
    }

    protected static interface To {
    }

    protected static enum ExpectedState {
        RESOLVED, CANCELLED, FAILED
    }
}