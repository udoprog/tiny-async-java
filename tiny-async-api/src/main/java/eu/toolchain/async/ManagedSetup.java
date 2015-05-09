package eu.toolchain.async;

public interface ManagedSetup<T> {
    public AsyncFuture<T> construct();

    public AsyncFuture<Void> destruct(T value);
}