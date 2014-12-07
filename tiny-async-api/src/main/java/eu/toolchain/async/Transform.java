package eu.toolchain.async;

public interface Transform<F, T> {
    T transform(F result) throws Exception;
}