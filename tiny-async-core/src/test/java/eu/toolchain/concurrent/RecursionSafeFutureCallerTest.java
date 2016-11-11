package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class RecursionSafeFutureCallerTest {
  private final Object result = new Object();
  private final Throwable cause = new Exception();

  private FutureCaller caller;

  private RecursionSafeFutureCaller underTest;

  @Mock
  private CompletionHandle<Object> done;
  @Mock
  private Runnable cancelled;
  @Mock
  private Runnable finished;
  @Mock
  private Consumer<Object> resolved;
  @Mock
  private Consumer<Throwable> failed;

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

    caller = mock(FutureCaller.class);
    underTest = new RecursionSafeFutureCaller(executor, caller);
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
