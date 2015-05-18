package eu.toolchain.async.concurrent;

import eu.toolchain.async.AsyncProducer;
import eu.toolchain.async.AsyncStream;
import eu.toolchain.async.AsyncSubscriber;

public class ConcurrentAsyncStream<T> implements AsyncStream<T> {
    private final AsyncProducer<T> producer;

    public ConcurrentAsyncStream(AsyncProducer<T> producer) {
        this.producer = producer;
    }

    @Override
    public AsyncSubscriber<T> subscribe(AsyncSubscriber<T> subscriber) {
        producer.produce(subscriber);
        return subscriber;
    }
}