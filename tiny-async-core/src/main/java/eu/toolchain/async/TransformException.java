package eu.toolchain.async;

/**
 * Indicates that a transform operation threw an exception.
 */
public class TransformException extends Exception {
    private static final long serialVersionUID = 5787592121819524920L;

    public TransformException(Throwable e) {
        super("error in transform", e);
    }

    public TransformException(Throwable error, Exception suppressed) {
        this(error);
        addSuppressed(suppressed);
    }
}