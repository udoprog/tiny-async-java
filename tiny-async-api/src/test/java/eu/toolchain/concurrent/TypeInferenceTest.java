package eu.toolchain.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests that all the expected type-inference combinations work.
 * <p>
 * This test is only valid at compile-time.
 *
 * @author udoprog
 */
@SuppressWarnings({"unchecked", "unused"})
public class TypeInferenceTest {
  private Async async;

  @Before
  public void setup() {
    this.async = Mockito.mock(Async.class);
  }

  @Test
  public void testFunctionInference() {
    final Stage<A> A = Mockito.mock(Stage.class);
    final Stage<B> B = Mockito.mock(Stage.class);

    final Function<A, OA> A2A = Mockito.mock(Function.class);
    final Function<A, OB> A2B = Mockito.mock(Function.class);

    final Function<B, OB> B2B = Mockito.mock(Function.class);
    final Function<B, OA> B2A = Mockito.mock(Function.class);

    // regular
    {
      final Stage<OA> f = A.thenApply(A2A);
    }

    // cast input B to A
    {
      final Stage<OA> f = B.thenApply(A2A);
    }

    // regular
    {
      final Stage<OB> f = B.thenApply(B2B);
    }

    // regular
    {
      final Stage<OB> f = A.thenApply(A2B);
    }

    // cast input B to A
    {
      final Stage<OB> f = A.thenApply(A2B);
    }
  }

  @Test
  public void testLazyFunctionInference() {
    final Stage<A> A = Mockito.mock(Stage.class);
    final Stage<B> B = Mockito.mock(Stage.class);

    final Function<A, Stage<OA>> A2A = Mockito.mock(Function.class);
    final Function<A, Stage<OB>> A2B = Mockito.mock(Function.class);

    final Function<B, Stage<OB>> B2B = Mockito.mock(Function.class);
    final Function<B, Stage<OA>> B2A = Mockito.mock(Function.class);

    // regular
    {
      final Stage<OA> f = A.thenCompose(A2A);
    }
    // cast input B to A
    {
      final Stage<OA> f = B.thenCompose(A2A);
    }

    // regular
    {
      final Stage<OB> f = B.thenCompose(B2B);
    }
    // regular
    {
      final Stage<OB> f = A.thenCompose(A2B);
    }
    // cast input B to A
    {
      final Stage<OB> f = B.thenCompose(A2B);
    }
  }

  @Test
  public void testCollectInference() {
    final List<Stage<A>> A = Mockito.mock(List.class);
    final List<Stage<B>> B = Mockito.mock(List.class);

    final Function<Collection<A>, OA> A2A = Mockito.mock(Function.class);
    final Function<Collection<A>, OB> A2B = Mockito.mock(Function.class);

    final Function<Collection<B>, OB> B2B = Mockito.mock(Function.class);
    final Function<Collection<B>, OA> B2A = Mockito.mock(Function.class);

    // regular
    {
      final Stage<Collection<A>> f = async.collect(A);
    }

    // cast input B to A
    {
      final Stage<Collection<A>> f = async.collect(B);
    }

    // regular
    {
      final Stage<OA> f = async.collect(A, A2A);
    }

    // cast input B to A
    {
      final Stage<OA> f = async.collect(B, A2A);
    }

    // regular
    {
      final Stage<OB> f = async.collect(B, B2B);
    }

    // regular
    {
      final Stage<OB> f = async.collect(A, A2B);
    }

    // cast input B to A
    {
      final Stage<OB> f = async.collect(B, A2B);
    }
  }

  @Test
  public void testCallable() {
    final Callable<A> A = Mockito.mock(Callable.class);
    final Callable<B> B = Mockito.mock(Callable.class);

    // regular
    {
      final Stage<A> f = async.call(A);
    }

    // cast B to A
    {
      final Stage<A> f = async.<A>call(B);
    }

    // regular
    {
      final Stage<B> f = async.call(B);
    }
  }

  @Test
  public void testError() {
    final Function<Throwable, A> A = Mockito.mock(Function.class);
    final Function<Throwable, B> B = Mockito.mock(Function.class);

    final Stage<A> a = Mockito.mock(Stage.class);

    {
      a.thenApplyFailed(A);
    }
    {
      a.thenApplyFailed(B);
    }
  }

  @Test
  public void testLazyError() {
    final Function<Throwable, Stage<A>> A = Mockito.mock(Function.class);
    final Function<Throwable, Stage<B>> B = Mockito.mock(Function.class);

    final Stage<A> a = Mockito.mock(Stage.class);

    {
      a.thenComposeFailed(A);
    }
  }

  @Test
  public void testLazyCancelled() {
    final Supplier<Stage<A>> A = Mockito.mock(Supplier.class);
    final Supplier<Stage<B>> B = Mockito.mock(Supplier.class);

    final Stage<A> a = Mockito.mock(Stage.class);

    {
      a.thenComposeCancelled(A);
    }
  }

  interface A {
  }

  interface B extends A {
  }

  interface OA {
  }

  interface OB extends OA {
  }
}
