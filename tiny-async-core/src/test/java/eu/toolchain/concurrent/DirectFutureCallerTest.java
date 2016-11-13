package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;

public class DirectFutureCallerTest {
  private static final Object reference = mock(Object.class);
  private static final RuntimeException e = new RuntimeException();

  private AtomicLong internalErrors;
  private DirectFutureCaller caller;

  private String errorMessage;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    internalErrors = new AtomicLong();

    errorMessage = null;

    caller = new DirectFutureCaller() {
      @Override
      protected void internalError(String what, Throwable e) {
        internalErrors.incrementAndGet();
        errorMessage = what;
      }
    };

    doReturn("foo").when(reference).toString();
  }

  @Test
  public void testRun() {
    final Runnable runnable = mock(Runnable.class);
    caller.execute(runnable);
    verify(runnable).run();
  }

  @Test
  public void testLeakedManagedReferenceEmptyStack() {
    final StackTraceElement[] empty = new StackTraceElement[0];
    caller.referenceLeaked(reference, empty);
    assertEquals(1, internalErrors.get());
    assertEquals("reference foo leaked @ unknown", errorMessage);
  }

  @Test
  public void testLeakedManagedReferenceUnknownStack() {
    final StackTraceElement[] unknown = null;
    caller.referenceLeaked(reference, unknown);
    assertEquals(1, internalErrors.get());
    assertEquals("reference foo leaked @ unknown", errorMessage);
  }

  @Test
  public void testLeakedManagedReferencePopulatedStack() {
    final StackTraceElement[] populated = new StackTraceElement[2];
    populated[0] = new StackTraceElement("SomeClass", "method", "file", 0);
    populated[1] = new StackTraceElement("SomeOtherClass", "method", "file", 0);
    caller.referenceLeaked(reference, populated);
    assertEquals(1, internalErrors.get());
    assertEquals(
        "reference foo leaked @ SomeClass.method (file:0)\n  SomeOtherClass.method " + "(file:0)",
        errorMessage);
  }
}
