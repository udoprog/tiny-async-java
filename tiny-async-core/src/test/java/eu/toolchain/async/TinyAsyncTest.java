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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TinyAsyncTest {
    @Rule
    public ExpectedException except = ExpectedException.none();

    private ExecutorService executor;
    private AsyncCaller caller;

    @Before
    public void testNullCaller() {
        except.expect(NullPointerException.class);
        except.expectMessage("caller");
        new TinyAsync(executor, null, null);
    }

    @Before
    public void setup() {
        executor = mock(ExecutorService.class);
        caller = mock(AsyncCaller.class);
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

    public void testDefaultExecutor() {
        final ExecutorService executor = mock(ExecutorService.class);
        final TinyAsync async = TinyAsync.builder().executor(executor).build();
        assertEquals(executor, async.defaultExecutor());
    }
}