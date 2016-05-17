package eu.toolchain.async;

import java.util.Collection;

/**
 * Simplified abstraction on top of CallbackGroup meant to reduce the result of multiple queries
 * into one. <p> <p> Will be called when the entire result is available which could be a memory hog.
 * If this is undesirable, use {@link StreamCollector}. </p>
 *
 * @param <S> source type to collect.
 * @param <T> target type to return when collection is done.
 * @author udoprog
 */
public interface Collector<S, T> {
    /**
     * Collect the given results by transforming them to the target type.
     *
     * @param results A collection of collected objects.
     * @return An implementation of the target type.
     * @throws Exception If unable to collect the results into a target.
     */
    T collect(Collection<S> results) throws Exception;
}
