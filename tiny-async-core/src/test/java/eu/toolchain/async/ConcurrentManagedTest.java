package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentManagedTest {
    private static final Object reference = new Object();
    private static final Throwable e = new Exception();

    private ConcurrentManaged<Object> managed;

    @Mock
    private AsyncFramework async;
    @Mock
    private ManagedSetup<Object> setup;
    @Mock
    private Borrowed<Object> borrowed;
    @Mock
    private ManagedAction<Object, Object> action;
    @Mock
    private ResolvableFuture<Void> startFuture;
    @Mock
    private ResolvableFuture<Void> zeroLeaseFuture;
    @Mock
    private ResolvableFuture<Object> stopReferenceFuture;
    @Mock
    private ResolvableFuture<Void> stopFuture;
    @Mock
    private AsyncFuture<Object> future;
    @Mock
    private AsyncFuture<Object> f;
    @Mock
    private FutureFinished finished;

    @Before
    public void setup() {
        managed = spy(new ConcurrentManaged<Object>(async, setup, startFuture, zeroLeaseFuture, stopReferenceFuture,
                stopFuture));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewManaged() throws Exception {
        final ResolvableFuture<Object> startFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> zeroLeaseFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> stopReferenceFuture = mock(ResolvableFuture.class);
        final ResolvableFuture<Object> stopFuture = mock(ResolvableFuture.class);

        when(async.future()).thenReturn(startFuture).thenReturn(zeroLeaseFuture).thenReturn(stopReferenceFuture);

        final AtomicReference<LazyTransform<Object, Object>> transform1 = new AtomicReference<>();

        doAnswer(new Answer<AsyncFuture<Object>>() {
            @Override
            public AsyncFuture<Object> answer(InvocationOnMock invocation) throws Throwable {
                transform1.set(invocation.getArgumentAt(0, LazyTransform.class));
                return stopFuture;
            }
        }).when(zeroLeaseFuture).transform(any(LazyTransform.class));

        final AtomicReference<LazyTransform<Object, Object>> transform2 = new AtomicReference<>();

        doAnswer(new Answer<AsyncFuture<Object>>() {
            @Override
            public AsyncFuture<Object> answer(InvocationOnMock invocation) throws Throwable {
                transform2.set(invocation.getArgumentAt(0, LazyTransform.class));
                return stopFuture;
            }
        }).when(stopReferenceFuture).transform(any(LazyTransform.class));

        ConcurrentManaged.newManaged(async, setup);

        verify(async, times(3)).future();
        verify(zeroLeaseFuture).transform(any(LazyTransform.class));
        verify(setup, never()).destruct(reference);
        verify(stopReferenceFuture, never()).transform(any(LazyTransform.class));

        transform1.get().transform(null);

        verify(setup, never()).destruct(reference);
        verify(stopReferenceFuture).transform(any(LazyTransform.class));

        transform2.get().transform(reference);

        verify(setup).destruct(reference);
    }

    private void setupDoto(boolean valid, boolean throwing) throws Exception {
        doReturn(borrowed).when(managed).borrow();
        doReturn(finished).when(borrowed).releasing();
        doReturn(valid).when(borrowed).isValid();
        doReturn(future).when(async).cancelled();
        doReturn(future).when(async).failed(e);
        doReturn(reference).when(borrowed).get();

        if (throwing) {
            doThrow(e).when(action).action(reference);
        } else {
            doReturn(f).when(action).action(reference);
        }

        doReturn(future).when(f).on(finished);
    }

    private void verifyDoto(boolean valid, boolean throwing) throws Exception {
        verify(managed).borrow();
        verify(borrowed).isValid();
        verify(async, times(!valid ? 1 : 0)).cancelled();
        verify(async, times(throwing ? 1 : 0)).failed(e);
        verify(borrowed, times(valid ? 1 : 0)).get();
        verify(borrowed, times(valid && !throwing ? 1 : 0)).releasing();
        verify(borrowed, times(throwing ? 1 : 0)).release();
        verify(action, times(valid ? 1 : 0)).action(reference);
        verify(f, times(valid && !throwing ? 1 : 0)).on(finished);
    }

    @Test
    public void testDotoInvalid() throws Exception {
        setupDoto(false, false);
        assertEquals(future, managed.doto(action));
        verifyDoto(false, false);
    }

    @Test
    public void testDotoValidThrows() throws Exception {
        setupDoto(true, true);
        assertEquals(future, managed.doto(action));
        verifyDoto(true, true);
    }

    @Test
    public void testDoto() throws Exception {
        setupDoto(true, false);
        assertEquals(future, managed.doto(action));
        verifyDoto(true, false);
    }
}