package eu.toolchain.async;

/**
 * Lazily transform the given value, into another value.
 *
 * @author udoprog
 *
 * @param <S> Source type to transform.
 * @param <T> Target type to transform.
 */
public interface LazyTransform<S, T> {
    /**
     * Lazily transform the given {@code value} into another value.
     *
     * @param result The value to transform.
     * @return A future that will be resolved with the transformed value.
     * @throws Exception if unable to process the given transformation, the target future will be failed.
     */
    AsyncFuture<T> transform(S result) throws Exception;
}