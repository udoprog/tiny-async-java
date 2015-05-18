package eu.toolchain.async;

public interface AsyncSubscriber<T> {
    public void resolve(T value);

    public void fail(Throwable cause);

    public void end();
}