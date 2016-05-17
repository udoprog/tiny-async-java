package eu.toolchain.async.helper;

import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.FutureDone;
import eu.toolchain.async.LazyTransform;
import eu.toolchain.async.ResolvableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class TransformHelperTestBase<T> {
    private LazyTransform<T, Object> transform;
    private ResolvableFuture<Object> target;
    private FutureDone<Object> done;

    private Throwable e;
    private T from;

    private final Object onResult = new Object();
    private final Exception onCause = new Exception();

    private int cancelledTimes;
    private int resolvedTimes;
    private int failedTimes;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        transform = mock(LazyTransform.class);
        target = mock(ResolvableFuture.class);
        done = setupDone(transform, target);
        e = setupError();
        from = setupFrom();
        cancelledTimes = setupCancelled();
        resolvedTimes = setupResolved();
        failedTimes = setupFailed();
    }

    protected abstract FutureDone<Object> setupDone(
        LazyTransform<T, Object> transform, ResolvableFuture<Object> target
    );

    protected Throwable setupError() {
        return new Exception();
    }

    protected int setupFailed() {
        return 0;
    }

    protected int setupResolved() {
        return 0;
    }

    protected int setupCancelled() {
        return 0;
    }

    protected T setupFrom() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailed() throws Exception {
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        if (failedTimes == 1) {
            setupVerifyFutureDone(f);
        }

        doReturn(f).when(transform).transform(from);
        done.failed(e);
        verify(target).fail(any(Exception.class));
        verify(f, times(failedTimes)).onDone(any(FutureDone.class));
    }

    @Test
    public void testResolvedThrows() throws Exception {
        doThrow(e).when(transform).transform(from);
        done.resolved(from);
        verify(target, times(Math.max(cancelledTimes, failedTimes))).resolve(from);
        verify(target, times(resolvedTimes)).fail(any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testResolved() throws Exception {
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        if (resolvedTimes == 1) {
            setupVerifyFutureDone(f);
        }

        doReturn(f).when(transform).transform(from);
        done.resolved(from);
        verify(target, times(Math.max(cancelledTimes, failedTimes))).resolve(from);
        verify(f, times(resolvedTimes)).onDone(any(FutureDone.class));
    }

    @Test
    public void testCancelledThrows() throws Exception {
        doThrow(e).when(transform).transform(from);
        done.cancelled();
        verify(target, times(Math.max(resolvedTimes, failedTimes))).cancel();
        verify(target, times(cancelledTimes)).fail(any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelled() throws Exception {
        final AsyncFuture<Object> f = mock(AsyncFuture.class);

        if (cancelledTimes == 1) {
            setupVerifyFutureDone(f);
        }

        doReturn(f).when(transform).transform(from);
        done.cancelled();
        verify(target, times(1)).cancel();
        verify(f, times(cancelledTimes)).onDone(any(FutureDone.class));
    }

    @SuppressWarnings("unchecked")
    private void setupVerifyFutureDone(final AsyncFuture<Object> f) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final FutureDone<Object> done = (FutureDone<Object>) invocation.getArguments()[0];
                done.cancelled();
                done.resolved(onResult);
                done.failed(onCause);

                verify(target).cancel();
                verify(target).resolve(onResult);
                verify(target).fail(onCause);
                return null;
            }
        }).when(f).onDone(any(FutureDone.class));
    }
}
