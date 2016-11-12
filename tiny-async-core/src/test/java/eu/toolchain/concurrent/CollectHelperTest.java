package eu.toolchain.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CollectHelperTest {
  @Mock
  private Function<Collection<From>, To> collector;
  @Mock
  private List<CompletionStage<From>> sources;
  @Mock
  private CompletableFuture<To> target;
  @Mock
  private From result;
  @Mock
  private Throwable e;
  @Mock
  private CompletionStage<From> f1;
  @Mock
  private CompletionStage<From> f2;

  private CollectHelper<From, To> helper;

  @Before
  public void setup() {
    this.helper = spy(new CollectHelper<>(1, collector, sources, target));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroSize() {
    new CollectHelper<From, To>(0, collector, sources, target);
  }

  @Test
  public void testResolved() throws Exception {
    doNothing().when(helper).add(any(Byte.class), anyObject());
    helper.resolved(result);
    verify(helper).add(CollectHelper.RESOLVED, result);
  }

  @Test
  public void testFailed() throws Exception {
    doNothing().when(helper).add(any(Byte.class), anyObject());
    doNothing().when(helper).checkFailed();

    helper.failed(e);

    verify(helper).add(CollectHelper.FAILED, e);
    verify(helper).checkFailed();
  }

  @Test
  public void testCancelled() throws Exception {
    doNothing().when(helper).add(any(Byte.class), anyObject());
    doNothing().when(helper).checkFailed();

    helper.cancelled();

    verify(helper).add(CollectHelper.CANCELLED, null);
    verify(helper).checkFailed();
  }

  @Test
  public void testCheckFailed() {
    final Iterator<CompletionStage<?>> futures =
        ImmutableList.<CompletionStage<?>>of(f1, f2).iterator();

    doReturn(futures).when(sources).iterator();

    assertFalse(helper.failed.get());
    assertNotNull(helper.sources);

    helper.checkFailed();

    assertTrue(helper.failed.get());
    assertNull(helper.sources);

    verify(f1, times(1)).cancel();
    verify(f2, times(1)).cancel();

    // should only fail once
    helper.checkFailed();

    assertTrue(helper.failed.get());
    assertNull(helper.sources);

    verify(f1, times(1)).cancel();
    verify(f2, times(1)).cancel();
  }

  @Test(expected = IllegalStateException.class)
  public void testAddWhenFinished() {
    helper.finished.set(true);
    helper.add(CollectHelper.RESOLVED, null);
  }

  @Test
  public void testAdd() {
    final CollectHelper<From, To>.Results r = mock(CollectHelper.Results.class);

    doReturn(r).when(helper).collect();
    doNothing().when(helper).writeAt(any(Integer.class), any(Byte.class), any());
    doNothing().when(helper).done(r);

    assertFalse(helper.finished.get());
    helper.add(CollectHelper.RESOLVED, null);
    assertTrue(helper.finished.get());

    verify(helper).collect();
    verify(helper).writeAt(0, CollectHelper.RESOLVED, null);
    verify(helper).done(r);
  }

  @Test(expected = IllegalStateException.class)
  public void testAddAlreadyFinished() {
    testAdd();
    helper.add(CollectHelper.RESOLVED, null);
  }

  private static interface From {
  }

  private static interface To {
  }
}
