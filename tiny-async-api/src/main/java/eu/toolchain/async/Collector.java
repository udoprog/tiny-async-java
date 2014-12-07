package eu.toolchain.async;

import java.util.Collection;

/**
 * Simplified abstraction on top of CallbackGroup meant to reduce the result of multiple queries into one.
 *
 * Will be called when the entire result is available. If this is undesirable, use {@link #StreamReducer}.
 *
 * @author udoprog
 */
public interface Collector<C, R> {
    R collect(Collection<C> results) throws Exception;
}