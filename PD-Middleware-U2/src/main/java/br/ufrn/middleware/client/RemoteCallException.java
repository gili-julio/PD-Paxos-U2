package br.ufrn.middleware.client;

/** Thrown by {@link RemoteClient} when a client-side call fails or returns non-2xx. */
public class RemoteCallException extends RuntimeException {
    private final int status;

    public RemoteCallException(int status, String message) {
        super(message);
        this.status = status;
    }
    public RemoteCallException(String message, Throwable cause) {
        super(message, cause);
        this.status = -1;
    }

    public int status() { return status; }
}
