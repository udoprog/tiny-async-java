package eu.toolchain.async;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RecursionSafeAsyncCallerTest {
    private final Object result = new Object();
    private final Throwable cause = new Exception();

    private AsyncCaller caller;

    private RecursionSafeAsyncCaller underTest;

    @Mock
    private FutureDone<Object> done;
    @Mock
    private FutureCancelled cancelled;
    @Mock
    private FutureFinished finished;
    @Mock
    private FutureResolved<Object> resolved;
    @Mock
    private FutureFailed failed;

    @Mock
    private StreamCollector<Object, Object> streamCollector;

    private StackTraceElement[] stack = new StackTraceElement[0];


    @Before
    public void setup() {
        ExecutorService executor = mock(ExecutorService.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(executor).submit(any(Runnable.class));

        caller = mock(AsyncCaller.class);
        underTest = new RecursionSafeAsyncCaller(executor, caller);
    }

    @Test
    public void testIsThreaded() {
        underTest.isThreaded();
        verify(caller).isThreaded();
    }

    @Test
    public void testResolveFutureDone() {
        underTest.resolve(done, result);
        verify(caller).resolve(done, result);
    }

    @Test
    public void testFailFutureDone() {
        underTest.fail(done, cause);
        verify(caller).fail(done, cause);
    }

    @Test
    public void testCancelFutureDone() {
        underTest.cancel(done);
        verify(caller).cancel(done);
    }

    @Test
    public void testRunFutureCancelled() {
        underTest.cancel(cancelled);
        verify(caller).cancel(cancelled);
    }

    @Test
    public void testRunFutureFinished() {
        underTest.finish(finished);
        verify(caller).finish(finished);
    }

    @Test
    public void testRunFutureResolved() {
        underTest.resolve(resolved, result);
        verify(caller).resolve(resolved, result);
    }

    @Test
    public void testRunFutureFailed() {
        underTest.fail(failed, cause);
        verify(caller).fail(failed, cause);
    }

    @Test
    public void testResolveStreamCollector() {
        underTest.resolve(streamCollector, result);
        verify(caller).resolve(streamCollector, result);
    }

    @Test
    public void testFailStreamCollector() {
        underTest.fail(streamCollector, cause);
        verify(caller).fail(streamCollector, cause);
    }

    @Test
    public void testCancelStreamCollector() {
        underTest.cancel(streamCollector);
        verify(caller).cancel(streamCollector);
    }

    @Test
    public void testLeakedManagedReference() {
        underTest.referenceLeaked(result, stack);
        verify(caller).referenceLeaked(result, stack);
    }

    @Test
    public void testExecute() {
        Runnable runnable = mock(Runnable.class);
        underTest.execute(runnable);
        verify(runnable).run();
    }
}
