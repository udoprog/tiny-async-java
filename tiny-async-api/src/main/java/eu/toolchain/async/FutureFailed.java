package eu.toolchain.async;

public interface FutureFailed {
    public void failed(Throwable error) throws Exception;
}