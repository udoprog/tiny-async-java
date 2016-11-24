package eu.toolchain.concurrent;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractImmediateTest {
  private static final RuntimeException other = new RuntimeException();
  private static final RuntimeException cause = new RuntimeException();

  @Mock
  private Caller caller;
  @Mock
  private Function<From, To> fn;
  @Mock
  private Function<From, Stage<To>> composeFn;
  @Mock
  private Supplier<From> supplier;
  @Mock
  private Supplier<Stage<From>> composeSupplier;
  @Mock
  private Function<Throwable, From> failedFn;
  @Mock
  private Function<Throwable, Stage<From>> composeFailedFn;
  @Mock
  private From from;
  @Mock
  private To to;
  @Mock
  private ImmediateFailed<From> ee;

  private AbstractImmediate<From> base;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    base = mock(AbstractImmediate.class, Mockito.CALLS_REAL_METHODS);
    base.caller = caller;
    doReturn(ee).when(base).executionExceptionFailed(any(Throwable.class), any(Throwable.class));
  }

  @Test
  public void immediateApply() throws Exception {
    doReturn(to).when(fn).apply(from);
    assertThat(base.thenApplyCompleted(fn, from), is(new ImmediateCompleted<>(caller, to)));
  }

  @Test
  public void immediateApplyThrows() throws Exception {
    doThrow(cause).when(fn).apply(from);
    assertThat(base.thenApplyCompleted(fn, from), is(new ImmediateFailed<>(caller, cause)));
  }

  @Test
  public void immediateCompose() throws Exception {
    final ImmediateCompleted<To> future = new ImmediateCompleted<>(caller, to);
    doReturn(future).when(composeFn).apply(from);
    assertThat(base.thenComposeCompleted(composeFn, from), is(future));
  }

  @Test
  public void immediateComposeThrows() throws Exception {
    doThrow(cause).when(composeFn).apply(from);
    assertThat(base.thenComposeCompleted(composeFn, from),
        is(new ImmediateFailed<>(caller, cause)));
  }

  @Test
  public void immediateCancelled() throws Exception {
    doReturn(from).when(supplier).get();
    assertThat(base.thenSupplyCancelledCancelled(supplier),
        is(new ImmediateCompleted<>(caller, from)));
  }

  @Test
  public void immediateCancelledThrows() throws Exception {
    doThrow(cause).when(supplier).get();
    assertThat(base.thenSupplyCancelledCancelled(supplier),
        is(new ImmediateFailed<>(caller, cause)));
  }

  @Test
  public void immediateComposeCancelled() throws Exception {
    final ImmediateCompleted<To> future = new ImmediateCompleted<>(caller, to);
    doReturn(future).when(composeSupplier).get();
    assertThat(base.thenComposeCancelledCancelled(composeSupplier), is(future));
  }

  @Test
  public void immediateComposeCancelledThrows() throws Exception {
    doThrow(cause).when(composeSupplier).get();
    assertThat(base.thenComposeCancelledCancelled(composeSupplier),
        is(new ImmediateFailed<>(caller, cause)));
  }

  @Test
  public void immediateFailed() throws Exception {
    doReturn(from).when(failedFn).apply(cause);
    assertThat(base.thenApplyCaughtFailed(failedFn, cause),
        is(new ImmediateCompleted<>(caller, from)));
  }

  @Test
  public void immediateFailedThrows() throws Exception {
    doThrow(other).when(failedFn).apply(cause);
    assertThat(base.thenApplyCaughtFailed(failedFn, cause), is(ee));
  }

  @Test
  public void immediateComposeFailed() throws Exception {
    final ImmediateCompleted<From> future = new ImmediateCompleted<>(caller, from);
    doReturn(future).when(composeFailedFn).apply(cause);
    assertThat(base.thenComposeFailedFailed(composeFailedFn, cause), is(future));
  }

  @Test
  public void immediateComposeFailedThrows() throws Exception {
    doThrow(other).when(composeFailedFn).apply(cause);
    assertThat(base.thenComposeFailedFailed(composeFailedFn, cause), is(ee));
  }

  public interface From {
  }

  public interface To {
  }
}
