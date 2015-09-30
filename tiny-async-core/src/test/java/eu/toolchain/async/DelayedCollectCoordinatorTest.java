package eu.toolchain.async;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

public class DelayedCollectCoordinatorTest {
    private AsyncCaller caller;
    private StreamCollector<Object, Object> collector;
    private Semaphore mutex;
    private ResolvableFuture<Object> future;
    private Callable<AsyncFuture<Object>> callable;
    private Callable<AsyncFuture<Object>> callable2;
    private AsyncFuture<Object> f;
    private AsyncFuture<Object> f2;

    private static final int PARALLELISM = 10;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        caller = mock(AsyncCaller.class);
        collector = mock(StreamCollector.class);
        mutex = mock(Semaphore.class);
        future = mock(ResolvableFuture.class);
        callable = mock(Callable.class);
        callable2 = mock(Callable.class);

        f = mock(AsyncFuture.class);
        f2 = mock(AsyncFuture.class);

        when(callable.call()).thenReturn(f);
        when(callable2.call()).thenReturn(f2);
    }

    @Test
    public void testCallFutureDoneMethods() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of();

        final DelayedCollectCoordinator<Object, Object> coordinator = new DelayedCollectCoordinator<Object, Object>(
                caller, callables, collector, mutex, future, 1);

        final Object result = new Object();
        final Throwable cause = new Throwable();

        coordinator.cancelled();
        coordinator.resolved(result);
        coordinator.failed(cause);

        verify(mutex, times(3)).release();
        verify(caller).cancel(collector);
        verify(caller).resolve(collector, result);
        verify(caller).fail(collector, cause);
    }

    @Test
    public void testEmptyCallables() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of();

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final Object reference = new Object();

        when(collector.end(0, 0, 0)).thenReturn(reference);

        coordinator.run();

        verify(f, never()).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(mutex, times(callables.size() + PARALLELISM)).acquire();

        verify(collector).end(0, 0, 0);

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future).resolve(reference);
        verify(future, never()).fail(any(Throwable.class));
        verify(future, never()).cancel();

        verify(caller, never()).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testInterrupted() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable);

        final InterruptedException e = new InterruptedException();

        doThrow(e).when(mutex).acquire();

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        coordinator.run();

        verify(f, never()).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector, never()).end(any(Integer.class), any(Integer.class), any(Integer.class));

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future, never()).resolve(any());
        verify(future).fail(e);
        verify(future, never()).cancel();

        verify(caller, never()).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testInterruptedOnCancelWait() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final InterruptedException e = new InterruptedException();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                Object[] args = invocation.getArguments();
                final FutureCancelled cancel = (FutureCancelled) args[0];
                cancel.cancelled();
                return null;
            }
        }).when(future).onCancelled(any(FutureCancelled.class));

        doThrow(e).when(mutex).acquire();

        coordinator.run();

        verify(f, never()).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector, never()).end(any(Integer.class), any(Integer.class), any(Integer.class));
        verify(mutex, times(callables.size())).acquire();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future).fail(e);
        verify(future, never()).resolve(any());
        verify(future, never()).cancel();

        verify(caller, times(1)).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testCancelled() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable, callable);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        doNothing().when(mutex).acquire();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                Object[] args = invocation.getArguments();
                final FutureCancelled cancel = (FutureCancelled) args[0];
                cancel.cancelled();
                return null;
            }
        }).when(future).onCancelled(any(FutureCancelled.class));

        coordinator.run();

        verify(f, never()).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector).end(0, 0, 2);
        verify(mutex, times(callables.size() + PARALLELISM)).acquire();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future, never()).fail(any(Throwable.class));
        verify(future).resolve(any());
        verify(future, never()).cancel();

        verify(caller, times(2)).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testSuccessful() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final Object reference = new Object();

        when(collector.end(1, 0, 0)).thenReturn(reference);
        when(callable.call()).thenReturn(f);

        coordinator.run();

        verify(f).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector).end(1, 0, 0);
        verify(mutex, times(callables.size() + PARALLELISM)).acquire();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future, never()).fail(any(Throwable.class));
        verify(future).resolve(reference);
        verify(future, never()).cancel();

        verify(caller, never()).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testFailSecond() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable, callable2);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final Object reference = new Object();

        final Exception e = new Exception();

        when(collector.end(1, 1, 0)).thenReturn(reference);
        when(callable.call()).thenReturn(f);
        when(callable2.call()).thenThrow(e);

        coordinator.run();

        verify(f).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector).end(1, 1, 0);
        verify(mutex, times(callables.size() + PARALLELISM)).acquire();
        verify(mutex).release();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future, never()).fail(any(Throwable.class));
        verify(future).resolve(reference);
        verify(future, never()).cancel();

        verify(caller, never()).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, times(1)).fail(eq(collector), any(Throwable.class));
    }

    /* If the first fails, should cancel the second. */
    @Test
    public void testFailFirst() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable, callable2);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final Object reference = new Object();

        final Exception e = new Exception();

        when(collector.end(0, 1, 1)).thenReturn(reference);
        when(callable.call()).thenThrow(e);
        when(callable2.call()).thenReturn(f);

        coordinator.run();

        verify(f, never()).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector).end(0, 1, 1);
        verify(mutex, times(callables.size() + PARALLELISM)).acquire();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future, never()).fail(any(Throwable.class));
        verify(future).resolve(reference);
        verify(future, never()).cancel();

        verify(caller, times(1)).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, times(1)).fail(eq(collector), any(Throwable.class));
    }

    @Test
    public void testCollectodEndThrows() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable);

        final DelayedCollectCoordinator<Object, Object> coordinator = setupCoordinator(callables);

        final Object reference = new Object();
        final Exception e = new Exception();

        when(collector.end(1, 0, 0)).thenThrow(e);
        when(callable.call()).thenReturn(f);

        coordinator.run();

        verify(f).onDone(coordinator);
        verify(f2, never()).onDone(coordinator);

        verify(collector).end(1, 0, 0);
        verify(mutex, times(callables.size() + PARALLELISM)).acquire();

        verify(future).onCancelled(any(FutureCancelled.class));
        verify(future).fail(any(Throwable.class));
        verify(future, never()).resolve(reference);
        verify(future, never()).cancel();

        verify(caller, never()).cancel(eq(collector));
        verify(caller, never()).resolve(eq(collector), any(Object.class));
        verify(caller, never()).fail(eq(collector), any(Throwable.class));
    }

    private DelayedCollectCoordinator<Object, Object> setupCoordinator(
            final List<Callable<AsyncFuture<Object>>> callables) {
        return new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, mutex, future, PARALLELISM);
    }
}