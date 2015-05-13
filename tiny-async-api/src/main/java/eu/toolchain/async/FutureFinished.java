package eu.toolchain.async;

/**
 * Handle to be called when a future is finished.
 *
 * A future is considered finished when it is <em>resolved</em>, <em>cancelled</em>, or <em>failed</em>.
 *
 * @author udoprog
 */
public interface FutureFinished {
    /**
     * Handle to be called when the future is finished.
     *
     * @throws Exception if unable to handle the finished future, will cause the target future to be failed.
     */
    void finished() throws Exception;
}