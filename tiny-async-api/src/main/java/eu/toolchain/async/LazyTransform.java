package eu.toolchain.async;

public interface LazyTransform<S, T> {
    AsyncFuture<T> transform(S result) throws Exception;
}