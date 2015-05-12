package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TinyAsyncTest {
    @Rule
    public ExpectedException except = ExpectedException.none();

    private ExecutorService executor;
    private AsyncCaller caller;
    private AsyncCaller threadedCaller;

    private StreamCollector<Object, Object> streamCollector;

    private AsyncFuture<Object> future;
    private Transform<Object, Object> transform;
    private LazyTransform<Object, Object> lazyTransform;
    private Transform<Throwable, Object> errorTransform;
    private LazyTransform<Throwable, Object> lazyErrorTransform;
    private Transform<Void, Object> cancelledTransform;
    private LazyTransform<Void, Object> lazyCancelledTransform;

    private TinyAsync underTest;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        executor = mock(ExecutorService.class);
        caller = mock(AsyncCaller.class);
        threadedCaller = mock(AsyncCaller.class);

        streamCollector = mock(StreamCollector.class);

        future = mock(AsyncFuture.class);

        transform = mock(Transform.class);
        lazyTransform = mock(LazyTransform.class);

        errorTransform = mock(Transform.class);
        lazyErrorTransform = mock(LazyTransform.class);

        cancelledTransform = mock(Transform.class);
        lazyCancelledTransform = mock(LazyTransform.class);

        underTest = new TinyAsync(executor, caller, threadedCaller);
    }

    @Test
    public void testGetDefaultExecutor() {
        assertEquals(executor, new TinyAsync(executor, caller, null).defaultExecutor());
    }

    @Test
    public void testGetCaller() {
        assertEquals(caller, new TinyAsync(null, caller, null).caller());
    }

    @Test
    public void testGetThreadedCaller() {
        assertEquals(threadedCaller, new TinyAsync(null, caller, threadedCaller).threadedCaller());
    }

    @Test
    public void testNullCaller() {
        except.expect(NullPointerException.class);
        except.expectMessage("caller");
        new TinyAsync(null, null, null);
    }

    @Test
    public void testMissingDefaultExecutorThrows() {
        except.expect(IllegalStateException.class);
        except.expectMessage("no default executor");
        new TinyAsync(null, caller, null).defaultExecutor();
    }

    @Test
    public void testMissingThreadedCallerThrows() {
        except.expect(IllegalStateException.class);
        except.expectMessage("no threaded caller");
        new TinyAsync(null, caller, null).threadedCaller();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransform() {
        underTest.transform(future, transform);
        verify(future).on(any(ResolvedTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyTransform() {
        underTest.transform(future, lazyTransform);
        verify(future).on(any(ResolvedLazyTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailure() {
        underTest.error(future, errorTransform);
        verify(future).on(any(FailedTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyFailure() {
        underTest.error(future, lazyErrorTransform);
        verify(future).on(any(FailedLazyTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelled() {
        underTest.cancelled(future, cancelledTransform);
        verify(future).on(any(CancelledTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyCancelled() {
        underTest.cancelled(future, lazyCancelledTransform);
        verify(future).on(any(CancelledLazyTransformHelper.class));
    }

    @Test
    public void testEventuallyCollectEmpty() throws Exception {
        final Object value = new Object();

        final List<Callable<AsyncFuture<Object>>> callables = new ArrayList<>();

        doReturn(value).when(streamCollector).end(0, 0, 0);
        final Object result = underTest.eventuallyCollect(callables, streamCollector, 10).getNow();

        assertEquals(value, result);
        verify(streamCollector).end(0, 0, 0);
    }

    @Test(expected = Throwable.class)
    public void testEventuallyCollectEmptyThrows() throws Exception {
        final Throwable e = new Throwable();

        final List<Callable<AsyncFuture<Object>>> callables = new ArrayList<>();

        doThrow(e).when(streamCollector).end(0, 0, 0);

        underTest.eventuallyCollect(callables, streamCollector, 10).getNow();
    }
}