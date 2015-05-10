package eu.toolchain.async;

import java.util.concurrent.Semaphore;

public class TinySemaphoreImpl implements TinySemaphore {
    private final Semaphore semaphore;

    public TinySemaphoreImpl(int permits) {
        this.semaphore = new Semaphore(permits);
    }

    @Override
    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    @Override
    public void release() {
        semaphore.release();
    }
}