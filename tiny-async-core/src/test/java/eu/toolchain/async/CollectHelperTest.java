package eu.toolchain.async;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class CollectHelperTest {
    private Collector<Object, Object> collector;
    private ResolvableFuture<Object> target;

    private final Object transformed = new Object();
    private final Object result = new Object();
    private final Exception e = new Exception();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        collector = mock(Collector.class);
        target = mock(ResolvableFuture.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroSize() {
        new CollectHelper<Object, Object>(0, collector, target);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOneFailed() throws Exception {
        final CollectHelper<Object, Object> helper = new CollectHelper<Object, Object>(2, collector, target);

        when(collector.collect(anyCollection())).thenReturn(transformed);

        helper.resolved(result);
        verify(target, never()).resolve(transformed);

        helper.failed(e);
        verify(target).fail(any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOneCancelled() throws Exception {
        final CollectHelper<Object, Object> helper = new CollectHelper<Object, Object>(2, collector, target);

        when(collector.collect(anyCollection())).thenReturn(transformed);

        helper.resolved(result);
        verify(target, never()).resolve(transformed);

        helper.cancelled();
        verify(target).cancel();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAllResolved() throws Exception {
        final CollectHelper<Object, Object> helper = new CollectHelper<Object, Object>(2, collector, target);

        when(collector.collect(anyCollection())).thenReturn(transformed);

        helper.resolved(result);
        verify(target, never()).resolve(transformed);

        helper.resolved(result);
        verify(target).resolve(transformed);
    }

    @Test
    public void testCollectThrows() throws Exception {
        final CollectHelper<Object, Object> helper = new CollectHelper<Object, Object>(1, collector, target);

        when(collector.collect(anyCollection())).thenThrow(e);

        helper.resolved(result);
        verify(target).fail(any(Exception.class));
    }

    public void testTooManyResolves() throws Exception {
        final CollectHelper<Object, Object> helper = new CollectHelper<Object, Object>(2, collector, target);

        when(collector.collect(anyCollection())).thenReturn(transformed);

        helper.resolved(result);
        verify(target, never()).resolve(transformed);

        helper.resolved(result);
        verify(target).resolve(transformed);

        helper.resolved(result);
    }
}