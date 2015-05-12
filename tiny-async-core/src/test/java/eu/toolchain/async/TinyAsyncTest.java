package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

public class TinyAsyncTest {
    private static final RuntimeException e = new RuntimeException();

    @Rule
    public ExpectedException except = ExpectedException.none();

    private Future<?> task;
    private ExecutorService executor;
    private AsyncCaller caller;
    private AsyncCaller threadedCaller;

    private Collector<Object, Object> collector;
    private StreamCollector<Object, Object> streamCollector;

    private ResolvableFuture<Object> resolvableFuture;

    private AsyncFuture<Object> future;
    private Transform<Object, Object> transform;
    private LazyTransform<Object, Object> lazyTransform;
    private Transform<Throwable, Object> errorTransform;
    private LazyTransform<Throwable, Object> lazyErrorTransform;
    private Transform<Void, Object> cancelledTransform;
    private LazyTransform<Void, Object> lazyCancelledTransform;

    private Callable<Object> callable;
    private Callable<AsyncFuture<Object>> cf;
    private Callable<AsyncFuture<Object>> cf2;

    private AsyncFuture<Object> f;
    private AsyncFuture<Object> f2;

    private TinyAsync underTest;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        task = mock(Future.class);
        executor = mock(ExecutorService.class);

        caller = mock(AsyncCaller.class);
        threadedCaller = mock(AsyncCaller.class);

        collector = mock(Collector.class);
        streamCollector = mock(StreamCollector.class);

        resolvableFuture = mock(ResolvableFuture.class);
        future = mock(AsyncFuture.class);

        transform = mock(Transform.class);
        lazyTransform = mock(LazyTransform.class);

        errorTransform = mock(Transform.class);
        lazyErrorTransform = mock(LazyTransform.class);

        cancelledTransform = mock(Transform.class);
        lazyCancelledTransform = mock(LazyTransform.class);

        callable = mock(Callable.class);
        cf = mock(Callable.class);
        cf2 = mock(Callable.class);

        f = mock(AsyncFuture.class);
        f2 = mock(AsyncFuture.class);

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
    public void testErrorTransform() {
        underTest.error(future, errorTransform);
        verify(future).on(any(FailedTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyErrorTransform() {
        underTest.error(future, lazyErrorTransform);
        verify(future).on(any(FailedLazyTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelledTransform() {
        underTest.cancelled(future, cancelledTransform);
        verify(future).on(any(CancelledTransformHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyCancelledTransform() {
        underTest.cancelled(future, lazyCancelledTransform);
        verify(future).on(any(CancelledLazyTransformHelper.class));
    }

    private void whenExecutorSubmitSetup() {
        doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                final Runnable run = (Runnable) invocation.getArguments()[0];
                run.run();
                return task;
            }
        }).when(executor).submit(any(Runnable.class));
    }

    private void verifyCall(int calls, int resolved, int failed, int taskCancel) throws Exception {
        verify(callable, times(calls)).call();
        verify(resolvableFuture, times(resolved)).resolve(any(Object.class));
        verify(resolvableFuture, times(failed)).fail(e);
        verify(task, times(taskCancel)).cancel(false);
    }

    @Test
    public void testCallFutureDone() throws Exception {
        whenExecutorSubmitSetup();
        when(resolvableFuture.isDone()).thenReturn(true);
        underTest.call(callable, executor, resolvableFuture);
        verifyCall(0, 0, 0, 0);
    }

    @Test
    public void testCall() throws Exception {
        whenExecutorSubmitSetup();
        when(resolvableFuture.isDone()).thenReturn(false);
        underTest.call(callable, executor, resolvableFuture);
        verifyCall(1, 1, 0, 0);
    }

    @Test
    public void testCallCallableThrows() throws Exception {
        whenExecutorSubmitSetup();
        when(callable.call()).thenThrow(e);
        when(resolvableFuture.isDone()).thenReturn(false);
        underTest.call(callable, executor, resolvableFuture);
        verifyCall(1, 0, 1, 0);
    }

    @Test
    public void testTaskCancel() throws Exception {
        doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                return task;
            }
        }).when(executor).submit(any(Runnable.class));

        when(callable.call()).thenThrow(e);
        when(resolvableFuture.isDone()).thenReturn(false);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final FutureCancelled cancelled = (FutureCancelled) invocation.getArguments()[0];
                cancelled.cancelled();
                return null;
            }
        }).when(resolvableFuture).on(any(FutureCancelled.class));

        underTest.call(callable, executor, resolvableFuture);
        verifyCall(0, 0, 0, 1);
    }

    @Test
    public void testCallSubmitThrows() throws Exception {
        when(executor.submit(any(Runnable.class))).thenThrow(e);
        when(resolvableFuture.isDone()).thenReturn(false);
        underTest.call(callable, executor, resolvableFuture);
        verifyCall(0, 0, 1, 0);
    }

    @Test
    public void testCollectEmpty() throws Exception {
        final List<AsyncFuture<Object>> futures = new ArrayList<>();
        final AsyncFuture<Collection<Object>> result = underTest.collect(futures);

        assertTrue(result.isDone());
        assertTrue(result.getNow().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectNonEmpty() {
        final List<AsyncFuture<Object>> futures = ImmutableList.of(f, f2);
        final AsyncFuture<Collection<Object>> result = underTest.collect(futures);

        verify(f).on(any(CollectHelper.class));
        verify(f2).on(any(CollectHelper.class));
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

    @Test
    public void testEventuallyCollectEmptyThrows() throws Exception {
        final Exception thrown = new Exception();

        final List<Callable<AsyncFuture<Object>>> callables = new ArrayList<>();

        doThrow(thrown).when(streamCollector).end(0, 0, 0);

        final AsyncFuture<Object> result = underTest.eventuallyCollect(callables, streamCollector, 10);
        assertTrue(result.isDone());
        assertFalse(result.isResolved());
        assertTrue(result.isFailed());
        assertFalse(result.isCancelled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventuallyCollectLessThanParallelism() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(cf, cf2);

        when(cf.call()).thenReturn(f);
        when(cf2.call()).thenReturn(f2);

        underTest.eventuallyCollect(callables, streamCollector, 4);

        verify(f).on(any(CollectHelper.class));
        verify(f2).on(any(CollectHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventuallyCollectLessThanParallelismOneThrows() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(cf, cf2);

        when(cf.call()).thenReturn(f);
        when(cf2.call()).thenThrow(e);

        underTest.eventuallyCollect(callables, streamCollector, 4);

        verify(f).on(any(CollectHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventuallyCollect() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(cf, cf2);

        when(cf.call()).thenReturn(f);
        when(cf2.call()).thenReturn(f2);

        underTest.eventuallyCollect(callables, streamCollector, 1);

        verify(executor).execute(any(DelayedCollectCoordinator.class));
        verify(f, never()).on(any(CollectHelper.class));
        verify(f2, never()).on(any(CollectHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectCollectorEmpty() throws Exception {
        final List<AsyncFuture<Object>> futures = ImmutableList.of();
        underTest.collect(futures, collector);

        verify(f, never()).on(any(CollectHelper.class));
        verify(f2, never()).on(any(CollectHelper.class));
        verify(collector).collect(any(Collection.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectCollector() throws Exception {
        final List<AsyncFuture<Object>> futures = ImmutableList.of(f, f2);
        underTest.collect(futures, collector);

        verify(f).on(any(CollectHelper.class));
        verify(f2).on(any(CollectHelper.class));
    }
}