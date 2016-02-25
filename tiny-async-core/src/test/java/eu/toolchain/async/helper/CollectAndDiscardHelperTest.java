package eu.toolchain.async.helper;

import eu.toolchain.async.ResolvableFuture;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CollectAndDiscardHelperTest {
    private static final int size = 2;

    private static final Object result = new Object();
    private static final Throwable cause = new Exception();

    private ResolvableFuture<Void> target;

    private CollectAndDiscardHelper<Object> helper;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        target = mock(ResolvableFuture.class);

        helper = new CollectAndDiscardHelper<Object>(size, target);
    }

    private void verifyTarget(int resolved, int failed, int cancelled) {
        verify(target, times(resolved)).resolve(null);
        verify(target, times(failed)).fail(any(Exception.class));
        verify(target, times(cancelled)).cancel();
    }

    @Test
    public void testResolved() throws Exception {
        helper.resolved(result);
        helper.resolved(result);

        verifyTarget(1, 0, 0);
    }

    @Test
    public void testFailed() throws Exception {
        helper.failed(cause);
        helper.resolved(result);

        verifyTarget(0, 1, 0);
    }

    @Test
    public void testCancelled() throws Exception {
        helper.resolved(result);
        helper.cancelled();

        verifyTarget(0, 0, 1);
    }
}
