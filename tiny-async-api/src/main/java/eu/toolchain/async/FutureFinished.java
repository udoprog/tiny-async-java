package eu.toolchain.async;

/**
 * Handle to implement when catching a future's transition into finished (any of failed or resolved).
 *
 */
public interface FutureFinished {
    void finished() throws Exception;
}