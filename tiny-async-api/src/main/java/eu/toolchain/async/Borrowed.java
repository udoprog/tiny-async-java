package eu.toolchain.async;

public interface Borrowed<T> extends AutoCloseable {
    public boolean isValid();

    public T get();

    public void release();

    public FutureFinished releasing();

    /**
     * Override of {@link AutoCloseable#close()} to remove throws signature.
     */
    @Override
    public void close();
}