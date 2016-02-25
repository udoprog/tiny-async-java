package eu.toolchain.async;

/**
 * Transform the given value, into another value.
 *
 * @param <S> Source type to transform.
 * @param <T> Target type to transform.
 * @author udoprog
 */
public interface Transform<S, T> {
    /**
     * Transform the given {@code value} into another value.
     *
     * @param result The value to transform.
     * @return The transformed value.
     * @throws Exception if unable to process the given transformation, the target future will be
     * failed.
     */
    T transform(S result) throws Exception;
}
