package eu.toolchain.async;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class DelayedCollectCoordinatorTest {
    private AsyncCaller caller;
    private StreamCollector<Object, Object> collector;
    private ResolvableFuture<Object> future;
    private Callable<AsyncFuture<Object>> callable;
    private Callable<AsyncFuture<Object>> callable2;
    private AsyncFuture<Object> f;
    private AsyncFuture<Object> f2;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        caller = mock(AsyncCaller.class);
        collector = mock(StreamCollector.class);
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

        final DelayedCollectCoordinator<Object, Object> coordinator =
                new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, future,
                        1);

        final Object result = new Object();
        final Throwable cause = new Throwable();

        coordinator.cancelled();
        coordinator.resolved(result);
        coordinator.failed(cause);

        verify(caller).cancel(collector);
        verify(caller).resolve(collector, result);
        verify(caller).fail(collector, cause);
    }

    @Test
    public void testBasic() throws Exception {
        final List<Callable<AsyncFuture<Object>>> callables = ImmutableList.of(callable, callable2);

        final DelayedCollectCoordinator<Object, Object> coordinator =
                new DelayedCollectCoordinator<Object, Object>(caller, callables, collector, future,
                        1);

        final Object result = new Object();
        final Throwable cause = new Throwable();

        coordinator.run();

        coordinator.resolved(result);
        verify(collector, never()).end(2, 0, 0);

        coordinator.resolved(result);
        verify(collector).end(2, 0, 0);

        verify(caller, never()).cancel(collector);
        verify(caller, times(2)).resolve(collector, result);
        verify(caller, never()).fail(collector, cause);
    }
}
