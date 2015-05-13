package eu.toolchain.async;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ExecutorAsyncCallerTest {
    private final Object result = new Object();
    private final Throwable cause = new Exception();

    private ExecutorService executor;
    private AsyncCaller caller;

    private ExecutorAsyncCaller underTest;

    private FutureDone<Object> done;
    private FutureCancelled cancelled;
    private FutureFinished finished;
    private FutureResolved<Object> resolved;
    private FutureFailed failed;

    private StreamCollector<Object, Object> streamCollector;

    private StackTraceElement[] stack = new StackTraceElement[0];

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        executor = mock(ExecutorService.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(executor).execute(any(Runnable.class));

        caller = mock(AsyncCaller.class);
        underTest = new ExecutorAsyncCaller(executor, caller);

        done = mock(FutureDone.class);
        cancelled = mock(FutureCancelled.class);
        finished = mock(FutureFinished.class);
        resolved = mock(FutureResolved.class);
        failed = mock(FutureFailed.class);

        streamCollector = mock(StreamCollector.class);
    }

    @Test
    public void testIsThreaded() {
        assertTrue(underTest.isThreaded());
    }

    @Test
    public void testResolveFutureDone() {
        underTest.resolve(done, result);
        verify(executor).execute(any(Runnable.class));
        verify(caller).resolve(done, result);
    }

    @Test
    public void testFailFutureDone() {
        underTest.fail(done, cause);
        verify(executor).execute(any(Runnable.class));
        verify(caller).fail(done, cause);
    }

    @Test
    public void testCancelFutureDone() {
        underTest.cancel(done);
        verify(executor).execute(any(Runnable.class));
        verify(caller).cancel(done);
    }

    @Test
    public void testRunFutureCancelled() {
        underTest.cancel(cancelled);
        verify(executor).execute(any(Runnable.class));
        verify(caller).cancel(cancelled);
    }

    @Test
    public void testRunFutureFinished() {
        underTest.finish(finished);
        verify(executor).execute(any(Runnable.class));
        verify(caller).finish(finished);
    }

    @Test
    public void testRunFutureResolved() {
        underTest.resolve(resolved, result);
        verify(executor).execute(any(Runnable.class));
        verify(caller).resolve(resolved, result);
    }

    @Test
    public void testRunFutureFailed() {
        underTest.fail(failed, cause);
        verify(executor).execute(any(Runnable.class));
        verify(caller).fail(failed, cause);
    }

    @Test
    public void testResolveStreamCollector() {
        underTest.resolve(streamCollector, result);
        verify(executor).execute(any(Runnable.class));
        verify(caller).resolve(streamCollector, result);
    }

    @Test
    public void testFailStreamCollector() {
        underTest.fail(streamCollector, cause);
        verify(executor).execute(any(Runnable.class));
        verify(caller).fail(streamCollector, cause);
    }

    @Test
    public void testCancelStreamCollector() {
        underTest.cancel(streamCollector);
        verify(executor).execute(any(Runnable.class));
        verify(caller).cancel(streamCollector);
    }

    @Test
    public void testLeakedManagedReference() {
        underTest.referenceLeaked(result, stack);
        verify(executor).execute(any(Runnable.class));
        verify(caller).referenceLeaked(result, stack);
    }
}