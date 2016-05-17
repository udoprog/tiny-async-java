package eu.toolchain.async;

import com.google.common.collect.ImmutableList;
import eu.toolchain.async.concurrent.ConcurrentManaged;
import eu.toolchain.async.concurrent.ConcurrentResolvableFuture;
import eu.toolchain.async.helper.CancelledLazyTransformHelper;
import eu.toolchain.async.helper.CancelledTransformHelper;
import eu.toolchain.async.helper.CollectAndDiscardHelper;
import eu.toolchain.async.helper.CollectHelper;
import eu.toolchain.async.helper.FailedLazyTransformHelper;
import eu.toolchain.async.helper.FailedTransformHelper;
import eu.toolchain.async.helper.ResolvedLazyTransformHelper;
import eu.toolchain.async.helper.ResolvedTransformHelper;
import eu.toolchain.async.immediate.ImmediateCancelledAsyncFuture;
import eu.toolchain.async.immediate.ImmediateFailedAsyncFuture;
import eu.toolchain.async.immediate.ImmediateResolvedAsyncFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TinyAsyncTest {
    private static final Object result = new Object();
    private static final RuntimeException e = new RuntimeException();

    @Rule
    public ExpectedException except = ExpectedException.none();

    @Mock
    private Future<?> task;
    @Mock
    private ExecutorService executor;
    @Mock
    private AsyncCaller caller;
    @Mock
    private AsyncCaller threadedCaller;
    @Mock
    private ClockSource clockSource;
    @Mock
    private Collector<Object, Object> collector;
    @Mock
    private StreamCollector<Object, Object> streamCollector;
    @Mock
    private ResolvableFuture<Object> resolvableFuture;
    @Mock
    private AsyncFuture<Object> future;
    @Mock
    private Callable<Object> callable;
    @Mock
    private Callable<AsyncFuture<Object>> cf;
    @Mock
    private Callable<AsyncFuture<Object>> cf2;
    @Mock
    private AsyncFuture<Object> f1;
    @Mock
    private AsyncFuture<Object> f2;
    @Mock
    private Callable<AsyncFuture<Object>> c;
    @Mock
    private Callable<AsyncFuture<Object>> c2;
    @Mock
    private Collection<Callable<AsyncFuture<Object>>> callables;
    @Mock
    private List<AsyncFuture<Object>> futures;

    private TinyAsync underTest;

    @Before
    public void setup() {
        underTest = spy(new TinyAsync(executor, caller, threadedCaller, null, clockSource));
    }

    @Test
    public void testBuilder() {
        assertTrue(TinyAsync.builder() instanceof TinyAsyncBuilder);
    }

    @Test
    public void testGetDefaultExecutor() {
        assertEquals(executor,
            new TinyAsync(executor, caller, null, null, clockSource).defaultExecutor());
    }

    @Test
    public void testGetCaller() {
        assertEquals(caller, new TinyAsync(null, caller, null, null, clockSource).caller());
    }

    @Test
    public void testGetThreadedCaller() {
        assertEquals(threadedCaller,
            new TinyAsync(null, caller, threadedCaller, null, clockSource).threadedCaller());
    }

    @Test
    public void testNullCaller() {
        except.expect(NullPointerException.class);
        except.expectMessage("caller");
        new TinyAsync(null, null, null, null, clockSource);
    }

    @Test
    public void testMissingDefaultExecutorThrows() {
        except.expect(IllegalStateException.class);
        except.expectMessage("no default executor");
        new TinyAsync(null, caller, null, null, clockSource).defaultExecutor();
    }

    @Test
    public void testMissingThreadedCallerThrows() {
        except.expect(IllegalStateException.class);
        except.expectMessage("no threaded caller");
        new TinyAsync(null, caller, null, null, clockSource).threadedCaller();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verifyTransform(Class<? extends FutureDone> done) {
        verify(underTest).future();
        verify(future).onDone(any(done));
        verify(resolvableFuture).bind(future);
    }

    @Test
    public void testTransform() {
        @SuppressWarnings("unchecked") final Transform<Object, Object> transform =
            mock(Transform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.transform(future, transform));
        verifyTransform(ResolvedTransformHelper.class);
    }

    @Test
    public void testLazyTransform() {
        @SuppressWarnings("unchecked") final LazyTransform<Object, Object> transform =
            mock(LazyTransform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.transform(future, transform));
        verifyTransform(ResolvedLazyTransformHelper.class);
    }

    @Test
    public void testErrorTransform() {
        @SuppressWarnings("unchecked") final Transform<Throwable, Object> transform =
            mock(Transform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.error(future, transform));
        verifyTransform(FailedTransformHelper.class);
    }

    @Test
    public void testLazyErrorTransform() {
        @SuppressWarnings("unchecked") final LazyTransform<Throwable, Object> transform =
            mock(LazyTransform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.error(future, transform));
        verifyTransform(FailedLazyTransformHelper.class);
    }

    @Test
    public void testCancelledTransform() {
        @SuppressWarnings("unchecked") final Transform<Void, Object> transform =
            mock(Transform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.cancelled(future, transform));
        verifyTransform(CancelledTransformHelper.class);
    }

    @Test
    public void testLazyCancelledTransform() {
        @SuppressWarnings("unchecked") final LazyTransform<Void, Object> transform =
            mock(LazyTransform.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(resolvableFuture).when(resolvableFuture).bind(future);

        assertEquals(resolvableFuture, underTest.cancelled(future, transform));
        verifyTransform(CancelledLazyTransformHelper.class);
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
    public void testCall1() throws Exception {
        @SuppressWarnings("unchecked") final Callable<Object> callable = mock(Callable.class);

        doReturn(executor).when(underTest).defaultExecutor();
        doReturn(resolvableFuture).when(underTest).future();
        doReturn(future).when(underTest).call(callable, executor, resolvableFuture);

        assertEquals(future, underTest.call(callable));

        verify(underTest).defaultExecutor();
        verify(underTest).future();
        verify(underTest).call(callable, executor, resolvableFuture);
    }

    @Test
    public void testLazyCall1() throws Exception {
        @SuppressWarnings("unchecked") final Callable<AsyncFuture<Object>> callable =
            mock(Callable.class);

        doReturn(executor).when(underTest).defaultExecutor();
        doReturn(future).when(underTest).lazyCall(callable, executor);

        assertEquals(future, underTest.lazyCall(callable));

        verify(underTest).defaultExecutor();
        verify(underTest).lazyCall(callable, executor);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLazyCall2() throws Exception {
        final Callable<AsyncFuture<Object>> callable = mock(Callable.class);
        final LazyTransform<AsyncFuture<Object>, Object> transform = mock(LazyTransform.class);
        final AsyncFuture<AsyncFuture<Object>> future = mock(AsyncFuture.class);
        final AsyncFuture<Object> target = mock(AsyncFuture.class);

        doReturn(transform).when(underTest).lazyCallTransform();
        doReturn(target).when(future).lazyTransform(transform);
        doReturn(future).when(underTest).call(callable, executor);

        assertEquals(target, underTest.lazyCall(callable, executor));

        verify(underTest).lazyCallTransform();
        verify(future).lazyTransform(transform);
        verify(underTest).call(callable, executor);
    }

    @Test
    public void testLazyCallTransform() {
        assertEquals(underTest.lazyCallTransform, underTest.lazyCallTransform());
    }

    @Test
    public void testCall2() throws Exception {
        @SuppressWarnings("unchecked") final Callable<Object> callable = mock(Callable.class);

        doReturn(resolvableFuture).when(underTest).future();
        doReturn(future).when(underTest).call(callable, executor, resolvableFuture);

        assertEquals(future, underTest.call(callable, executor));

        verify(underTest).future();
        verify(underTest).call(callable, executor, resolvableFuture);
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
        }).when(resolvableFuture).onCancelled(any(FutureCancelled.class));

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

    private void runEventuallyCollectTest(int size, int parallelism) {
        setupEventuallyCollect(size, parallelism);
        assertEquals(future, underTest.eventuallyCollect(callables, streamCollector, parallelism));
        verifyEventuallyCollect(size, parallelism);
    }

    private void setupEventuallyCollect(int size, int parallelism) {
        doReturn(size == 0).when(callables).isEmpty();
        doReturn(size).when(callables).size();
        doReturn(future).when(underTest).doEventuallyCollectImmediate(callables, streamCollector);
        doReturn(future).when(underTest).doEventuallyCollectEmpty(streamCollector);
        doReturn(future)
            .when(underTest)
            .doEventuallyCollect(callables, streamCollector, parallelism);
    }

    private void verifyEventuallyCollect(int size, int parallelism) {
        verify(callables, times(1)).isEmpty();
        verify(callables, times(size > 0 ? 1 : 0)).size();
        verify(underTest, times(size == 0 ? 1 : 0)).doEventuallyCollectEmpty(streamCollector);
        verify(underTest,
            times(size > 0 && size < parallelism ? 1 : 0)).doEventuallyCollectImmediate(callables,
            streamCollector);
        verify(underTest, times(size >= parallelism ? 1 : 0)).doEventuallyCollect(callables,
            streamCollector, 10);
    }

    @Test
    public void testEventuallyCollectEmpty() throws Exception {
        runEventuallyCollectTest(0, 10);
    }

    @Test
    public void testEventuallyCollectLessThanParallelism() throws Exception {
        runEventuallyCollectTest(1, 10);
    }

    @Test
    public void testEventuallyCollect() throws Exception {
        runEventuallyCollectTest(20, 10);
    }

    @Test
    public void testDoEventuallyCollectEmptyThrows() throws Exception {
        doThrow(e).when(streamCollector).end(0, 0, 0);
        doReturn(future).when(underTest).resolved(result);
        doReturn(future).when(underTest).failed(e);

        assertEquals(future, underTest.doEventuallyCollectEmpty(streamCollector));

        verify(streamCollector).end(0, 0, 0);
        verify(underTest, never()).resolved(result);
        verify(underTest).failed(e);
    }

    @Test
    public void testDoEventuallyCollectEmpty() throws Exception {
        doReturn(result).when(streamCollector).end(0, 0, 0);
        doReturn(future).when(underTest).resolved(result);
        doReturn(future).when(underTest).failed(e);

        assertEquals(future, underTest.doEventuallyCollectEmpty(streamCollector));

        verify(streamCollector).end(0, 0, 0);
        verify(underTest).resolved(result);
        verify(underTest, never()).failed(e);
    }

    @Test
    public void testDoEventuallyCollectImmediate() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(c, c2);
        final List<AsyncFuture<Object>> futures = ImmutableList.of(f1, f2);

        doReturn(f1).when(c).call();
        doThrow(e).when(c2).call();
        doReturn(f2).when(underTest).failed(e);
        doReturn(future).when(underTest).collect(futures, streamCollector);

        assertEquals(future, underTest.doEventuallyCollectImmediate(callables, streamCollector));

        verify(c).call();
        verify(c2).call();
        verify(underTest).failed(e);
        verify(underTest).collect(futures, streamCollector);
    }

    @Test
    public void testDoEventuallyCollect() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(c, c2);

        doReturn(executor).when(underTest).defaultExecutor();
        doReturn(resolvableFuture).when(underTest).future();

        assertEquals(resolvableFuture,
            underTest.doEventuallyCollect(callables, streamCollector, 10));

        verify(executor).execute(any(DelayedCollectCoordinator.class));
        verify(underTest).defaultExecutor();
    }

    @Test
    public void testCollectDefaultCollectionEmpty() {
        doReturn(true).when(futures).isEmpty();
        doReturn(future).when(underTest).resolved(anyCollection());
        doReturn(collector).when(underTest).collection();
        doReturn(future).when(underTest).collect(futures, collector);

        assertEquals(future, underTest.collect(futures));

        final InOrder order = inOrder(futures, underTest);
        order.verify(futures).isEmpty();
        order.verify(underTest).resolved(anyCollection());
        order.verify(underTest, never()).collection();
        order.verify(underTest, never()).collect(futures, collector);
    }

    @Test
    public void testCollectDefaultCollectionNonEmpty() {
        doReturn(false).when(futures).isEmpty();
        doReturn(future).when(underTest).resolved(anyCollection());
        doReturn(collector).when(underTest).collection();
        doReturn(future).when(underTest).collect(futures, collector);

        assertEquals(future, underTest.collect(futures));

        final InOrder order = inOrder(futures, underTest);
        order.verify(futures).isEmpty();
        order.verify(underTest, never()).resolved(anyCollection());
        order.verify(underTest).collection();
        order.verify(underTest).collect(futures, collector);
    }

    @Test
    public void testCollectEmpty() throws Exception {
        doReturn(true).when(futures).isEmpty();
        doReturn(future).when(underTest).doCollectEmpty(collector);
        doReturn(future).when(underTest).doCollect(futures, collector);

        assertEquals(future, underTest.collect(futures, collector));

        final InOrder order = inOrder(futures, underTest);
        order.verify(futures).isEmpty();
        order.verify(underTest).doCollectEmpty(collector);
        order.verify(underTest, never()).doCollect(futures, collector);
    }

    @Test
    public void testCollect() throws Exception {
        doReturn(false).when(futures).isEmpty();
        doReturn(future).when(underTest).doCollectEmpty(collector);
        doReturn(future).when(underTest).doCollect(futures, collector);

        assertEquals(future, underTest.collect(futures, collector));

        final InOrder order = inOrder(futures, underTest);
        order.verify(futures).isEmpty();
        order.verify(underTest, never()).doCollectEmpty(collector);
        order.verify(underTest).doCollect(futures, collector);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoCollect() throws Exception {
        final Collection<AsyncFuture<Object>> futures = ImmutableList.of(f1, f2);

        doReturn(resolvableFuture).when(underTest).future();
        doNothing().when(underTest).bindSignals(resolvableFuture, futures);

        assertEquals(resolvableFuture, underTest.doCollect(futures, collector));

        verify(underTest).future();
        verify(underTest).bindSignals(resolvableFuture, futures);
        verify(f1).onDone(any(CollectHelper.class));
        verify(f2).onDone(any(CollectHelper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoCollectEmpty() throws Exception {
        doReturn(result).when(collector).collect(anyCollection());
        doReturn(future).when(underTest).resolved(result);

        assertEquals(future, underTest.doCollectEmpty(collector));

        verify(underTest).resolved(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoCollectEmptyThrows() throws Exception {
        doThrow(e).when(collector).collect(anyCollection());
        doReturn(future).when(underTest).failed(e);

        assertEquals(future, underTest.doCollectEmpty(collector));

        verify(underTest).failed(e);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectStreamEmpty() throws Exception {
        final Collection<AsyncFuture<Object>> futures = mock(Collection.class);
        doReturn(true).when(futures).isEmpty();
        doReturn(future).when(underTest).doCollectEmpty(streamCollector);
        doReturn(future).when(underTest).doCollect(futures, streamCollector);

        assertEquals(future, underTest.collect(futures, streamCollector));

        verify(futures).isEmpty();
        verify(underTest).doCollectEmpty(streamCollector);
        verify(underTest, never()).doCollect(futures, streamCollector);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectStream() throws Exception {
        final Collection<AsyncFuture<Object>> futures = mock(Collection.class);
        doReturn(false).when(futures).isEmpty();
        doReturn(future).when(underTest).doCollectEmpty(streamCollector);
        doReturn(future).when(underTest).doCollect(futures, streamCollector);

        assertEquals(future, underTest.collect(futures, streamCollector));

        verify(futures).isEmpty();
        verify(underTest, never()).doCollectEmpty(streamCollector);
        verify(underTest).doCollect(futures, streamCollector);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoCollectStream() throws Exception {
        final Collection<AsyncFuture<Object>> futures = ImmutableList.of(f1, f2);

        doReturn(resolvableFuture).when(underTest).future();
        doNothing().when(underTest).bindSignals(resolvableFuture, futures);

        assertEquals(resolvableFuture, underTest.doCollect(futures, streamCollector));

        verify(underTest).future();
        verify(underTest).bindSignals(resolvableFuture, futures);
        verify(f1).onDone(any(CollectHelper.class));
        verify(f2).onDone(any(CollectHelper.class));
    }

    @Test
    public void testDoCollectEmptyStream() throws Exception {
        doReturn(result).when(streamCollector).end(0, 0, 0);
        doReturn(future).when(underTest).resolved(result);

        assertEquals(future, underTest.doCollectEmpty(streamCollector));

        verify(underTest).resolved(result);
    }

    @Test
    public void testDoCollectEmptyStreamThrows() throws Exception {
        doThrow(e).when(streamCollector).end(0, 0, 0);
        doReturn(future).when(underTest).failed(e);

        assertEquals(future, underTest.doCollectEmpty(streamCollector));

        verify(underTest).failed(e);
    }

    @Test
    public void testCollectAndDiscardEmpty() throws Exception {
        final Collection<AsyncFuture<Object>> futures = mock(Collection.class);
        doReturn(true).when(futures).isEmpty();
        doReturn(future).when(underTest).resolved();
        doReturn(future).when(underTest).doCollectAndDiscard(futures);

        assertEquals(future, underTest.collectAndDiscard(futures));

        verify(futures).isEmpty();
        verify(underTest).resolved();
        verify(underTest, never()).doCollectAndDiscard(futures);
    }

    @Test
    public void testCollectAndDiscard() throws Exception {
        final Collection<AsyncFuture<Object>> futures = mock(Collection.class);
        doReturn(false).when(futures).isEmpty();
        doReturn(future).when(underTest).resolved();
        doReturn(future).when(underTest).doCollectAndDiscard(futures);

        assertEquals(future, underTest.collectAndDiscard(futures));

        verify(futures).isEmpty();
        verify(underTest, never()).resolved();
        verify(underTest).doCollectAndDiscard(futures);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoCollectAndDiscard() throws Exception {
        final Collection<AsyncFuture<Object>> futures = ImmutableList.of(f1, f2);

        doReturn(resolvableFuture).when(underTest).future();
        doNothing().when(underTest).bindSignals(resolvableFuture, futures);

        assertEquals(resolvableFuture, underTest.doCollectAndDiscard(futures));

        verify(underTest).future();
        verify(underTest).bindSignals(resolvableFuture, futures);
        verify(f1).onDone(any(CollectAndDiscardHelper.class));
        verify(f2).onDone(any(CollectAndDiscardHelper.class));
    }

    @Test
    public void testFuture() {
        assertTrue(underTest.future() instanceof ConcurrentResolvableFuture);
    }

    @Test
    public void testResolved0() {
        doReturn(future).when(underTest).resolved(null);
        assertEquals(future, underTest.resolved());
        verify(underTest).resolved(null);
    }

    @Test
    public void testResolved() {
        assertTrue(underTest.resolved(null) instanceof ImmediateResolvedAsyncFuture);
    }

    @Test
    public void testFailed() {
        assertTrue(underTest.failed(e) instanceof ImmediateFailedAsyncFuture);
    }

    @Test
    public void testCancelled() {
        assertTrue(underTest.cancelled() instanceof ImmediateCancelledAsyncFuture);
    }

    @Test
    public void testManaged() {
        final ManagedSetup<Object> setup = mock(ManagedSetup.class);
        assertTrue(underTest.managed(setup) instanceof ConcurrentManaged);
    }

    @Test
    public void testBindSignals() {
        final Collection<AsyncFuture<Object>> futures = ImmutableList.of(f1, f2);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final FutureCancelled cancelled =
                    invocation.getArgumentAt(0, FutureCancelled.class);
                cancelled.cancelled();
                return null;
            }
        }).when(future).onCancelled(any(FutureCancelled.class));

        underTest.bindSignals(future, futures);

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(f1).cancel();
        verify(f2).cancel();
    }
}
