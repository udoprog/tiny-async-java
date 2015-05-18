package eu.toolchain.async;

public interface AsyncProducer<T> {
    public void produce(AsyncSubscriber<T> subscriber);
}