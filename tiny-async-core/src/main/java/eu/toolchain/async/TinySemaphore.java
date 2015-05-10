package eu.toolchain.async;

/**
 * Tiny semaphore abstraction with a simplified interface.
 */
public interface TinySemaphore {
    public void acquire() throws InterruptedException;

    public void release();
}