package eu.toolchain.async;

public interface LazyTransform<C, R> {
    AsyncFuture<R> transform(C result) throws Exception;
}