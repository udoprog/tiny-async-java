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
public class RecursionSafeCallerTest {
  private final Object result = new Object();
  private final Throwable cause = new Exception();

  private Caller caller;

  private RecursionSafeCaller underTest;

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

    caller = mock(Caller.class);
    underTest = new RecursionSafeCaller(executor, caller);
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
