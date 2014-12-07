package eu.toolchain.async;

public interface StreamCollector<C, R> {
    /**
     * Implement to trigger on one resolved future.
     *
     * @param result The result of the future.
     */
    void resolved(C result) throws Exception;

    /**
     * Implement to trigger on one failed future.
     *
     * @param cause The cause of the failed future.
     */
    void failed(Throwable cause) throws Exception;

    /**
     * Implement to trigger on one cancelled future.
     */
    void cancelled() throws Exception;

    /**
     * Implement to fire when all callbacks have been resolved.
     *
     * @param resolved How many futures were resolved.
     * @param failed How many futures were failed.
     */
    R end(int resolved, int failed, int cancelled) throws Exception;
}