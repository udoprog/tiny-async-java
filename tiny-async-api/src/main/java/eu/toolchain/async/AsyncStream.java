package eu.toolchain.async;

public interface AsyncStream<T> {
    public AsyncSubscriber<T> subscribe(AsyncSubscriber<T> subsciber);
}