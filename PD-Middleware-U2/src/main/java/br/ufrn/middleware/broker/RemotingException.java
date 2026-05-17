package br.ufrn.middleware.broker;

/**
 * Remoting Error pattern. Carries an HTTP status to surface to the client.
 */
public class RemotingException extends RuntimeException {
    private final int httpStatus;

    public RemotingException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public RemotingException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() { return httpStatus; }

    public static RemotingException badRequest(String msg) { return new RemotingException(400, msg); }
    public static RemotingException notFound(String msg)   { return new RemotingException(404, msg); }
    public static RemotingException conflict(String msg)   { return new RemotingException(409, msg); }
    public static RemotingException internal(String msg, Throwable t) { return new RemotingException(500, msg, t); }
}
