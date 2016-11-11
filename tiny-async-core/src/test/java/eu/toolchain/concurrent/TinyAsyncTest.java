package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
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

import com.google.common.collect.ImmutableList;
import eu.toolchain.concurrent.concurrent.ConcurrentCompletableFuture;
import eu.toolchain.concurrent.concurrent.ConcurrentManaged;
import eu.toolchain.concurrent.helper.CollectAndDiscardHelper;
import eu.toolchain.concurrent.helper.CollectHelper;
import eu.toolchain.concurrent.immediate.ImmediateCancelledStage;
import eu.toolchain.concurrent.immediate.ImmediateCompletedStage;
import eu.toolchain.concurrent.immediate.ImmediateFailedStage;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
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
  private FutureCaller caller;
  @Mock
  private FutureCaller threadedCaller;
  @Mock
  private ClockSource clockSource;
  @Mock
  private Function<Collection<Object>, Object> collector;
  @Mock
  private StreamCollector<Object, Object> streamCollector;
  @Mock
  private CompletableFuture<Object> completableFuture;
  @Mock
  private CompletionStage<Object> future;
  @Mock
  private Callable<Object> callable;
  @Mock
  private Callable<CompletionStage<Object>> cf;
  @Mock
  private Callable<CompletionStage<Object>> cf2;
  @Mock
  private CompletionStage<Object> f1;
  @Mock
  private CompletionStage<Object> f2;
  @Mock
  private Callable<CompletionStage<Object>> c;
  @Mock
  private Callable<CompletionStage<Object>> c2;
  @Mock
  private Collection<Callable<CompletionStage<Object>>> callables;
  @Mock
  private List<CompletionStage<Object>> futures;

  private TinyFuture underTest;

  @Before
  public void setup() {
    underTest = spy(new TinyFuture(executor, null, caller, clockSource));
  }

  @Test
  public void testBuilder() {
    assertTrue(TinyFuture.builder() instanceof TinyFutureBuilder);
  }

  @Test
  public void testGetDefaultExecutor() {
    assertEquals(executor, new TinyFuture(executor, null, caller, clockSource).executor());
  }

  @Test
  public void testGetCaller() {
    assertEquals(caller, new TinyFuture(null, null, caller, clockSource).caller());
  }

  @Test
  public void testNullCaller() {
    except.expect(NullPointerException.class);
    except.expectMessage("caller");
    new TinyFuture(null, null, null, clockSource);
  }

  @Test
  public void testMissingDefaultExecutorThrows() {
    except.expect(IllegalStateException.class);
    except.expectMessage("no default executor");
    new TinyFuture(null, null, caller, clockSource).executor();
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
    verify(completableFuture, times(resolved)).complete(any(Object.class));
    verify(completableFuture, times(failed)).fail(e);
    verify(task, times(taskCancel)).cancel(false);
  }

  @Test
  public void testCallFutureDone() throws Exception {
    whenExecutorSubmitSetup();
    when(completableFuture.isDone()).thenReturn(true);
    underTest.doCall(callable, executor, completableFuture);
    verifyCall(0, 0, 0, 0);
  }

  @Test
  public void testCall1() throws Exception {
    @SuppressWarnings("unchecked") final Callable<Object> callable = mock(Callable.class);

    doReturn(executor).when(underTest).executor();
    doReturn(completableFuture).when(underTest).future();
    doReturn(future).when(underTest).doCall(callable, executor, completableFuture);

    assertEquals(future, underTest.call(callable));

    verify(underTest).executor();
    verify(underTest).future();
    verify(underTest).doCall(callable, executor, completableFuture);
  }

  @Test
  public void testCall2() throws Exception {
    @SuppressWarnings("unchecked") final Callable<Object> callable = mock(Callable.class);

    doReturn(completableFuture).when(underTest).future();
    doReturn(future).when(underTest).doCall(callable, executor, completableFuture);

    assertEquals(future, underTest.call(callable, executor));

    verify(underTest).future();
    verify(underTest).doCall(callable, executor, completableFuture);
  }

  @Test
  public void testCall() throws Exception {
    whenExecutorSubmitSetup();
    when(completableFuture.isDone()).thenReturn(false);
    underTest.doCall(callable, executor, completableFuture);
    verifyCall(1, 1, 0, 0);
  }

  @Test
  public void testCallCallableThrows() throws Exception {
    whenExecutorSubmitSetup();
    when(callable.call()).thenThrow(e);
    when(completableFuture.isDone()).thenReturn(false);
    underTest.doCall(callable, executor, completableFuture);
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
    when(completableFuture.isDone()).thenReturn(false);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Runnable cancelled = (Runnable) invocation.getArguments()[0];
        cancelled.run();
        return null;
      }
    }).when(completableFuture).whenCancelled(any(Runnable.class));

    underTest.doCall(callable, executor, completableFuture);
    verifyCall(0, 0, 0, 1);
  }

  @Test
  public void testCallSubmitThrows() throws Exception {
    when(executor.submit(any(Runnable.class))).thenThrow(e);
    when(completableFuture.isDone()).thenReturn(false);
    underTest.doCall(callable, executor, completableFuture);
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
    doReturn(future).when(underTest).doEventuallyCollect(callables, streamCollector, parallelism);
  }

  private void verifyEventuallyCollect(int size, int parallelism) {
    verify(callables, times(1)).isEmpty();
    verify(callables, times(size > 0 ? 1 : 0)).size();
    verify(underTest, times(size == 0 ? 1 : 0)).doEventuallyCollectEmpty(streamCollector);
    verify(underTest, times(size > 0 && size < parallelism ? 1 : 0)).doEventuallyCollectImmediate(
        callables, streamCollector);
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
    doReturn(future).when(underTest).completed(result);
    doReturn(future).when(underTest).failed(e);

    assertEquals(future, underTest.doEventuallyCollectEmpty(streamCollector));

    verify(streamCollector).end(0, 0, 0);
    verify(underTest, never()).completed(result);
    verify(underTest).failed(e);
  }

  @Test
  public void testDoEventuallyCollectEmpty() throws Exception {
    doReturn(result).when(streamCollector).end(0, 0, 0);
    doReturn(future).when(underTest).completed(result);
    doReturn(future).when(underTest).failed(e);

    assertEquals(future, underTest.doEventuallyCollectEmpty(streamCollector));

    verify(streamCollector).end(0, 0, 0);
    verify(underTest).completed(result);
    verify(underTest, never()).failed(e);
  }

  @Test
  public void testDoEventuallyCollectImmediate() throws Exception {
    final List<Callable<CompletionStage<Object>>> callables = ImmutableList.of(c, c2);
    final List<CompletionStage<Object>> futures = ImmutableList.of(f1, f2);

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
    final List<Callable<CompletionStage<Object>>> callables = ImmutableList.of(c, c2);

    doReturn(executor).when(underTest).executor();
    doReturn(completableFuture).when(underTest).future();

    assertEquals(completableFuture, underTest.doEventuallyCollect(callables, streamCollector, 10));

    verify(executor).execute(any(DelayedCollectCoordinator.class));
    verify(underTest).executor();
  }

  @Test
  public void testCollectDefaultCollectionEmpty() {
    doReturn(true).when(futures).isEmpty();
    doReturn(future).when(underTest).completed(anyCollection());
    doReturn(future).when(underTest).collect(futures, collector);

    assertEquals(future, underTest.collect(futures));

    final InOrder order = inOrder(futures, underTest);
    order.verify(futures).isEmpty();
    order.verify(underTest).completed(anyCollection());
    order.verify(underTest, never()).collect(futures, collector);
  }

  @Test
  public void testCollectDefaultCollectionNonEmpty() {
    doReturn(false).when(futures).isEmpty();
    doReturn(future).when(underTest).completed(anyCollection());
    doReturn(future).when(underTest).collect(eq(futures), any(Function.class));

    assertEquals(future, underTest.collect(futures));

    final InOrder order = inOrder(futures, underTest);
    order.verify(futures).isEmpty();
    order.verify(underTest, never()).completed(anyCollection());
    order.verify(underTest).collect(eq(futures), any(Function.class));
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
    final Collection<CompletionStage<Object>> futures = ImmutableList.of(f1, f2);

    doReturn(completableFuture).when(underTest).future();
    doNothing().when(underTest).bindSignals(completableFuture, futures);

    assertEquals(completableFuture, underTest.doCollect(futures, collector));

    verify(underTest).future();
    verify(underTest).bindSignals(completableFuture, futures);
    verify(f1).handle(any(CollectHelper.class));
    verify(f2).handle(any(CollectHelper.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDoCollectEmpty() throws Exception {
    doReturn(result).when(collector).apply(anyCollection());
    doReturn(future).when(underTest).completed(result);

    assertEquals(future, underTest.doCollectEmpty(collector));

    verify(underTest).completed(result);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDoCollectEmptyThrows() throws Exception {
    doThrow(e).when(collector).apply(anyCollection());
    doReturn(future).when(underTest).failed(e);

    assertEquals(future, underTest.doCollectEmpty(collector));

    verify(underTest).failed(e);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCollectStreamEmpty() throws Exception {
    final Collection<CompletionStage<Object>> futures = mock(Collection.class);
    doReturn(true).when(futures).isEmpty();
    doReturn(future).when(underTest).doStreamCollectEmpty(streamCollector);
    doReturn(future).when(underTest).doStreamCollect(futures, streamCollector);

    assertEquals(future, underTest.collect(futures, streamCollector));

    verify(futures).isEmpty();
    verify(underTest).doStreamCollectEmpty(streamCollector);
    verify(underTest, never()).doStreamCollect(futures, streamCollector);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCollectStream() throws Exception {
    final Collection<CompletionStage<Object>> futures = mock(Collection.class);
    doReturn(false).when(futures).isEmpty();
    doReturn(future).when(underTest).doStreamCollectEmpty(streamCollector);
    doReturn(future).when(underTest).doStreamCollect(futures, streamCollector);

    assertEquals(future, underTest.collect(futures, streamCollector));

    verify(futures).isEmpty();
    verify(underTest, never()).doStreamCollectEmpty(streamCollector);
    verify(underTest).doStreamCollect(futures, streamCollector);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDoCollectStream() throws Exception {
    final Collection<CompletionStage<Object>> futures = ImmutableList.of(f1, f2);

    doReturn(completableFuture).when(underTest).future();
    doNothing().when(underTest).bindSignals(completableFuture, futures);

    assertEquals(completableFuture, underTest.doStreamCollect(futures, streamCollector));

    verify(underTest).future();
    verify(underTest).bindSignals(completableFuture, futures);
    verify(f1).handle(any(CollectHelper.class));
    verify(f2).handle(any(CollectHelper.class));
  }

  @Test
  public void testDoCollectEmptyStream() throws Exception {
    doReturn(result).when(streamCollector).end(0, 0, 0);
    doReturn(future).when(underTest).completed(result);

    assertEquals(future, underTest.doStreamCollectEmpty(streamCollector));

    verify(underTest).completed(result);
  }

  @Test
  public void testDoCollectEmptyStreamThrows() throws Exception {
    doThrow(e).when(streamCollector).end(0, 0, 0);
    doReturn(future).when(underTest).failed(e);

    assertEquals(future, underTest.doStreamCollectEmpty(streamCollector));

    verify(underTest).failed(e);
  }

  @Test
  public void testCollectAndDiscardEmpty() throws Exception {
    final Collection<CompletionStage<Object>> futures = mock(Collection.class);
    doReturn(true).when(futures).isEmpty();
    doReturn(future).when(underTest).completed();
    doReturn(future).when(underTest).doCollectAndDiscard(futures);

    assertEquals(future, underTest.collectAndDiscard(futures));

    verify(futures).isEmpty();
    verify(underTest).completed();
    verify(underTest, never()).doCollectAndDiscard(futures);
  }

  @Test
  public void testCollectAndDiscard() throws Exception {
    final Collection<CompletionStage<Object>> futures = mock(Collection.class);
    doReturn(false).when(futures).isEmpty();
    doReturn(future).when(underTest).completed();
    doReturn(future).when(underTest).doCollectAndDiscard(futures);

    assertEquals(future, underTest.collectAndDiscard(futures));

    verify(futures).isEmpty();
    verify(underTest, never()).completed();
    verify(underTest).doCollectAndDiscard(futures);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDoCollectAndDiscard() throws Exception {
    final Collection<CompletionStage<Object>> futures = ImmutableList.of(f1, f2);

    doReturn(completableFuture).when(underTest).future();
    doNothing().when(underTest).bindSignals(completableFuture, futures);

    assertEquals(completableFuture, underTest.doCollectAndDiscard(futures));

    verify(underTest).future();
    verify(underTest).bindSignals(completableFuture, futures);
    verify(f1).handle(any(CollectAndDiscardHelper.class));
    verify(f2).handle(any(CollectAndDiscardHelper.class));
  }

  @Test
  public void testFuture() {
    assertTrue(underTest.future() instanceof ConcurrentCompletableFuture);
  }

  @Test
  public void testResolved0() {
    doReturn(future).when(underTest).completed(null);
    assertEquals(future, underTest.completed());
    verify(underTest).completed(null);
  }

  @Test
  public void testResolved() {
    assertTrue(underTest.completed() instanceof ImmediateCompletedStage);
  }

  @Test
  public void testFailed() {
    assertTrue(underTest.failed(e) instanceof ImmediateFailedStage);
  }

  @Test
  public void testCancelled() {
    assertTrue(underTest.cancelled() instanceof ImmediateCancelledStage);
  }

  @Test
  public void testManaged() {
    final Supplier<? extends CompletionStage<Object>> setup = mock(Supplier.class);
    final Function<? super Object, ? extends CompletionStage<Void>> teardown = mock(Function.class);
    assertTrue(underTest.managed(setup, teardown) instanceof ConcurrentManaged);
  }

  @Test
  public void testBindSignals() {
    final Collection<CompletionStage<Object>> futures = ImmutableList.of(f1, f2);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        final Runnable cancelled = invocation.getArgumentAt(0, Runnable.class);
        cancelled.run();
        return null;
      }
    }).when(future).whenCancelled(any(Runnable.class));

    underTest.bindSignals(future, futures);

    verify(future).whenCancelled(any(Runnable.class));
    verify(f1).cancel();
    verify(f2).cancel();
  }
}
