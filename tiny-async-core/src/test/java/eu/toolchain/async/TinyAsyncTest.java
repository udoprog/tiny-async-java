package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;

public class TinyAsyncTest {
    private ExecutorService executor;

    @Before
    public void setup() {
        executor = mock(ExecutorService.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventuallyCollectEmpty() throws Exception {
        final TinyAsync async = TinyAsync.builder().executor(executor).build();

        final Object value = new Object();

        final List<Callable<AsyncFuture<Object>>> callables = new ArrayList<>();
        final StreamCollector<Object, Object> collector = mock(StreamCollector.class);

        doReturn(value).when(collector).end(0, 0, 0);
        final Object result = async.eventuallyCollect(callables, collector, 10).getNow();

        assertEquals(value, result);
        verify(collector).end(0, 0, 0);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = Throwable.class)
    public void testEventuallyCollectEmptyThrows() throws Exception {
        final TinyAsync async = TinyAsync.builder().executor(executor).build();

        final Throwable e = new Throwable();

        final List<Callable<AsyncFuture<Object>>> callables = new ArrayList<>();
        final StreamCollector<Object, Object> collector = mock(StreamCollector.class);

        doThrow(e).when(collector).end(0, 0, 0);

        async.eventuallyCollect(callables, collector, 10).getNow();
    }
}