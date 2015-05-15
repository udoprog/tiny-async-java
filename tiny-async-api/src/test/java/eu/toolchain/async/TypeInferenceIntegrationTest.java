package eu.toolchain.async;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests that all the expected type-inference combinations work.
 *
 * This test is only valid at compile-time.
 *
 * @author udoprog
 */
@SuppressWarnings({ "unchecked", "unused" })
public class TypeInferenceIntegrationTest {
    private AsyncFramework async;

    @Before
    public void setup() {
        this.async = Mockito.mock(AsyncFramework.class);
    }

    @Test
    public void testTransformInference() {
        final AsyncFuture<A> A = Mockito.mock(AsyncFuture.class);
        final AsyncFuture<B> B = Mockito.mock(AsyncFuture.class);

        final Transform<A, OA> A2A = Mockito.mock(Transform.class);
        final Transform<A, OB> A2B = Mockito.mock(Transform.class);

        final Transform<B, OB> B2B = Mockito.mock(Transform.class);
        final Transform<B, OA> B2A = Mockito.mock(Transform.class);

        // @formatter:off
        { final AsyncFuture<OA> f = async.transform(A, A2A); } // regular
        { final AsyncFuture<OA> f = async.transform(B, A2A); } // cast input B to A

        { final AsyncFuture<OB> f = async.transform(B, B2B); } // regular
        { final AsyncFuture<OB> f = async.transform(A, A2B); } // regular
        { final AsyncFuture<OB> f = async.transform(B, A2B); } // cast input B to A
        // @formatter:on
    }

    @Test
    public void testLazyTransformInference() {
        final AsyncFuture<A> A = Mockito.mock(AsyncFuture.class);
        final AsyncFuture<B> B = Mockito.mock(AsyncFuture.class);

        final LazyTransform<A, OA> A2A = Mockito.mock(LazyTransform.class);
        final LazyTransform<A, OB> A2B = Mockito.mock(LazyTransform.class);

        final LazyTransform<B, OB> B2B = Mockito.mock(LazyTransform.class);
        final LazyTransform<B, OA> B2A = Mockito.mock(LazyTransform.class);

        // @formatter:off
        { final AsyncFuture<OA> f = async.transform(A, A2A); } // regular
        { final AsyncFuture<OA> f = async.transform(B, A2A); } // cast input B to A

        { final AsyncFuture<OB> f = async.transform(B, B2B); } // regular
        { final AsyncFuture<OB> f = async.transform(A, A2B); } // regular
        { final AsyncFuture<OB> f = async.transform(B, A2B); } // cast input B to A
        // @formatter:on
    }

    @Test
    public void testCollectInference() {
        final List<AsyncFuture<A>> A = Mockito.mock(List.class);
        final List<AsyncFuture<B>> B = Mockito.mock(List.class);

        final Collector<A, OA> A2A = Mockito.mock(Collector.class);
        final Collector<A, OB> A2B = Mockito.mock(Collector.class);

        final Collector<B, OB> B2B = Mockito.mock(Collector.class);
        final Collector<B, OA> B2A = Mockito.mock(Collector.class);

        // @formatter:off
        { final AsyncFuture<Collection<A>> f = async.collect(A); } // regular
        { final AsyncFuture<Collection<A>> f = async.<A> collect(B); } // cast input B to A
        // @formatter:on

        // @formatter:off
        { final AsyncFuture<OA> f = async.collect(A, A2A); } // regular
        { final AsyncFuture<OA> f = async.collect(B, A2A); } // cast input B to A

        { final AsyncFuture<OB> f = async.collect(B, B2B); } // regular
        { final AsyncFuture<OB> f = async.collect(A, A2B); } // regular
        { final AsyncFuture<OB> f = async.collect(B, A2B); } // cast input B to A
        // @formatter:on
    }

    @Test
    public void testCallable() {
        final Callable<A> A = Mockito.mock(Callable.class);
        final Callable<B> B = Mockito.mock(Callable.class);

        // @formatter:off
        { final AsyncFuture<A> f = async.call(A); } // regular
        { final AsyncFuture<A> f = async.<A> call(B); } // cast B to A
        { final AsyncFuture<B> f = async.call(B); } // regular
        // @formatter:on
    }

    @Test
    public void testError() {
        final Transform<Throwable, A> A = Mockito.mock(Transform.class);
        final Transform<Throwable, B> B = Mockito.mock(Transform.class);

        final AsyncFuture<A> a = Mockito.mock(AsyncFuture.class);

        { a.catchFailed(A); }
        { a.catchFailed(B); }
    }

    @Test
    public void testLazyError() {
        final LazyTransform<Throwable, A> A = Mockito.mock(LazyTransform.class);
        final LazyTransform<Throwable, B> B = Mockito.mock(LazyTransform.class);

        final AsyncFuture<A> a = Mockito.mock(AsyncFuture.class);

        { a.lazyCatchFailed(A); }
        // TODO: make this valid?
        // { a.error(B); }
    }

    @Test
    public void testLazyCancelled() {
        final LazyTransform<Void, A> A = Mockito.mock(LazyTransform.class);
        final LazyTransform<Void, B> B = Mockito.mock(LazyTransform.class);

        final AsyncFuture<A> a = Mockito.mock(AsyncFuture.class);

        { a.lazyCatchCancelled(A); }
        // TODO: make this valid?
        // { a.error(B); }
    }

    private static interface A {
    }

    private static interface B extends A {
    }

    private static interface OA {
    }

    private static interface OB extends OA {
    }
}