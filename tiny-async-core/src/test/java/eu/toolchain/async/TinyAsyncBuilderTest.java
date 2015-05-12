package eu.toolchain.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TinyAsyncBuilderTest {
    @Rule
    public ExpectedException except = ExpectedException.none();

    private ExecutorService executor;
    private AsyncCaller caller;

    @Before
    public void setup() {
        executor = mock(ExecutorService.class);
        caller = mock(AsyncCaller.class);
    }

    @Test
    public void testBuilderNullExecutor() {
        except.expect(NullPointerException.class);
        except.expectMessage("executor");
        builder().executor(null);
    }

    @Test
    public void testBuilderNullCallerExecutor() {
        except.expect(NullPointerException.class);
        except.expectMessage("callerExecutor");
        builder().callerExecutor(null);
    }

    @Test
    public void testBuilderNullCaller() {
        except.expect(NullPointerException.class);
        except.expectMessage("caller");
        builder().caller(null);
    }

    @Test
    public void testBuilderThreadedWithoutExecutor() {
        except.expect(IllegalStateException.class);
        except.expectMessage("no executor");
        builder().threaded(true).build();
    }

    @Test
    public void testBuilderThreadedExecutor() {
        final TinyAsync async = builder().threaded(true).executor(executor).build();
        assertEquals(executor, async.defaultExecutor());
    }

    @Test
    public void testBuilderDefaultDirectCaller() {
        final TinyAsync async = builder().build();
        assertTrue("if not caller configured, using StderrDefaultAsyncCaller",
                async.caller() instanceof PrintStreamDefaultAsyncCaller);
    }

    @Test
    public void testBuilderCaller() {
        final TinyAsync async = builder().caller(caller).build();
        assertEquals(caller, async.caller());
    }

    @Test
    public void testBuilderDefaultExecutor() {
        final TinyAsync async = builder().executor(executor).build();
        assertEquals(executor, async.defaultExecutor());
    }

    @Test
    public void testBuilderCallerExecutor() {
        final TinyAsync async = builder().callerExecutor(executor).build();
        assertNotNull(async.threadedCaller());
    }

    @Test
    public void setupThreadedCaller() {
        final AsyncCaller caller = mock(AsyncCaller.class);
        when(caller.isThreaded()).thenReturn(true);
        final TinyAsync async = builder().callerExecutor(executor).threaded(true).caller(caller).build();
    }

    private TinyAsyncBuilder builder() {
        return new TinyAsyncBuilder();
    }
}