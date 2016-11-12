package eu.toolchain.concurrent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class DirectFutureCallerTest {
  private static final Object reference = mock(Object.class);
  private static final RuntimeException e = new RuntimeException();

  private AtomicLong internalErrors;
  private DirectFutureCaller caller;
  private CompletionHandle<Object> done;
  private CompletionHandle<Object> throwingDone;

  private Runnable finished;
  private Runnable throwingFinished;

  private Runnable cancelled;
  private Runnable throwingCancelled;

  private Consumer<Throwable> failed;
  private Consumer<Throwable> throwingFailed;

  private Consumer<Object> resolved;
  private Consumer<Object> throwingResolved;

  private StreamCollector<Object, Object> streamCollector;
  private StreamCollector<Object, Object> throwingStreamCollector;

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

    done = mock(CompletionHandle.class);

    throwingDone = mock(CompletionHandle.class);
    doThrow(e).when(throwingDone).cancelled();
    doThrow(e).when(throwingDone).resolved(reference);
    doThrow(e).when(throwingDone).failed(any(Throwable.class));

    finished = mock(Runnable.class);
    throwingFinished = mock(Runnable.class);
    doThrow(e).when(throwingFinished).run();

    cancelled = mock(Runnable.class);
    throwingCancelled = mock(Runnable.class);
    doThrow(e).when(throwingCancelled).run();

    failed = mock(Consumer.class);
    throwingFailed = mock(Consumer.class);
    doThrow(e).when(throwingFailed).accept(e);

    resolved = mock(Consumer.class);
    throwingResolved = mock(Consumer.class);
    doThrow(e).when(throwingResolved).accept(reference);

    streamCollector = mock(StreamCollector.class);
    throwingStreamCollector = mock(StreamCollector.class);
    doThrow(e).when(throwingStreamCollector).completed(reference);
    doThrow(e).when(throwingStreamCollector).cancelled();
    doThrow(e).when(throwingStreamCollector).failed(e);

    doReturn("foo").when(reference).toString();
  }

  @Test
  public void testResolveFutureDone() throws Exception {
    caller.complete(done, reference);
    verify(done).resolved(reference);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.complete(throwingDone, reference);
    verify(throwingDone).resolved(reference);
    assertEquals(1, internalErrors.get());
    assertEquals("CompletionHandle#completed(T)", errorMessage);
  }

  @Test
  public void testCancelFutureDone() throws Exception {
    caller.cancel(done);
    verify(done).cancelled();
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.cancel(throwingDone);
    verify(throwingDone).cancelled();
    assertEquals(1, internalErrors.get());
    assertEquals("CompletionHandle#cancelled()", errorMessage);
  }

  @Test
  public void testFailFutureDone() throws Exception {
    caller.fail(done, e);
    verify(done).failed(e);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.fail(throwingDone, e);
    verify(throwingDone).failed(e);
    assertEquals(1, internalErrors.get());
    assertEquals("CompletionHandle#failed(Throwable)", errorMessage);
  }

  @Test
  public void testRunFutureFinished() throws Exception {
    caller.finish(finished);
    verify(finished).run();
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.finish(throwingFinished);
    verify(throwingFinished).run();
    assertEquals(1, internalErrors.get());
    assertEquals("FutureFinished#finished()", errorMessage);
  }

  @Test
  public void testRunFutureCancelled() throws Exception {
    caller.cancel(cancelled);
    verify(cancelled).run();
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.cancel(throwingCancelled);
    verify(throwingCancelled).run();
    assertEquals(1, internalErrors.get());
    assertEquals("FutureCancelled#cancelled()", errorMessage);
  }

  @Test
  public void testRunFutureFailed() throws Exception {
    caller.fail(failed, e);
    verify(failed).accept(e);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.fail(throwingFailed, e);
    verify(throwingFailed).accept(e);
    assertEquals(1, internalErrors.get());
    assertEquals("FutureFailed#failed(Throwable)", errorMessage);
  }

  @Test
  public void testRunFutureResolved() throws Exception {
    caller.complete(resolved, reference);
    verify(resolved).accept(reference);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.complete(throwingResolved, reference);
    verify(throwingResolved).accept(reference);
    assertEquals(1, internalErrors.get());
    assertEquals("FutureResolved#completed(T)", errorMessage);
  }

  @Test
  public void testResolveStreamCollector() throws Exception {
    caller.complete(streamCollector, reference);
    verify(streamCollector).completed(reference);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.complete(throwingStreamCollector, reference);
    verify(throwingStreamCollector).completed(reference);
    assertEquals(1, internalErrors.get());
    assertEquals("StreamCollector#completed(T)", errorMessage);
  }

  @Test
  public void testFailStreamCollector() throws Exception {
    caller.fail(streamCollector, e);
    verify(streamCollector).failed(e);
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.fail(throwingStreamCollector, e);
    verify(throwingStreamCollector).failed(e);
    assertEquals(1, internalErrors.get());
    assertEquals("StreamCollector#failed(Throwable)", errorMessage);
  }

  @Test
  public void testCancelStreamCollector() throws Exception {
    caller.cancel(streamCollector);
    verify(streamCollector).cancelled();
    assertEquals(0, internalErrors.get());
    assertEquals(null, errorMessage);

    caller.cancel(throwingStreamCollector);
    verify(throwingStreamCollector).cancelled();
    assertEquals(1, internalErrors.get());
    assertEquals("StreamCollector#cancel()", errorMessage);
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
