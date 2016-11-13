package eu.toolchain.concurrent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ExecutorFutureCallerTest {
  private final Object result = new Object();
  private final Throwable cause = new Exception();

  private ExecutorService executor;
  private FutureCaller caller;

  private ExecutorFutureCaller underTest;

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

    caller = mock(FutureCaller.class);
    underTest = new ExecutorFutureCaller(executor, caller);
  }

  @Test
  public void testExecute() {
    final Runnable runnable = mock(Runnable.class);
    underTest.execute(runnable);
    verify(executor).execute(runnable);
    verify(runnable).run();
  }

  @Test
  public void testLeakedManagedReference() {
    underTest.referenceLeaked(result, stack);
    verify(executor).execute(any(Runnable.class));
    verify(caller).referenceLeaked(result, stack);
  }
}
