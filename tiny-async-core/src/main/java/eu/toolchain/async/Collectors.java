package eu.toolchain.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common collector implementations.
 */
public final class Collectors {
    private static final Collector<?, ? extends List<?>> LIST = new Collector<Object, List<Object>>() {
        @Override
        public List<Object> collect(Collection<Object> results) throws Exception {
            return new ArrayList<Object>(results);
        }
    };

    /**
     * A collector that maps T -> List<T>.
     */
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, List<T>> list() {
        return (Collector<T, List<T>>) LIST;
    }

    private static final Collector<? extends Set<?>, ? extends Set<?>> JOIN_SETS = new Collector<Set<Object>, Set<Object>>() {
        @Override
        public Set<Object> collect(Collection<Set<Object>> results) throws Exception {
            final Set<Object> all = new HashSet<Object>();

            for (final Set<Object> result : results)
                all.addAll(result);

            return all;
        }
    };

    /**
     * A collector that maps Set<T> -> Set<T> using a join operations.
     */
    @SuppressWarnings("unchecked")
    public static <T> Collector<Set<T>, Set<T>> joinSets() {
        return (Collector<Set<T>, Set<T>>) JOIN_SETS;
    }

    private static final Collector<? extends List<?>, ? extends List<?>> JOIN_LISTS = new Collector<List<Object>, List<Object>>() {
        @Override
        public List<Object> collect(Collection<List<Object>> results) throws Exception {
            final List<Object> list = new ArrayList<Object>();

            for (final List<Object> part : results)
                list.addAll(part);

            return list;
        }

    };

    @SuppressWarnings("unchecked")
    public static <T> Collector<List<T>, List<T>> joinLists() {
        return (Collector<List<T>, List<T>>) JOIN_LISTS;
    }

    private static final Collector<?, ? extends Collection<?>> COLLECTION = new Collector<Object, Collection<Object>>() {
        @Override
        public Collection<Object> collect(Collection<Object> results) throws Exception {
            return results;
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, Collection<T>> collection() {
        return (Collector<T, Collection<T>>) COLLECTION;
    }
}
