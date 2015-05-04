package eu.toolchain.async;

public interface FutureResolved<T> {
    public void resolved(T value) throws Exception;
}