package br.ufrn.middleware.identification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Absolute Object Reference (AOR) - protocol + host + port + object id.
 * Wire form: {@code <protocol>://<host>:<port>/<objectId>}.
 */
public final class AbsoluteObjectReference {
    private final String protocol;
    private final String host;
    private final int port;
    private final ObjectId objectId;

    public AbsoluteObjectReference(String protocol, String host, int port, ObjectId objectId) {
        this.protocol = Objects.requireNonNull(protocol);
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.objectId = Objects.requireNonNull(objectId);
    }

    public static AbsoluteObjectReference parse(String s) {
        try {
            URI u = new URI(s);
            String oid = u.getPath();
            if (oid != null && oid.startsWith("/")) oid = oid.substring(1);
            return new AbsoluteObjectReference(u.getScheme(), u.getHost(), u.getPort(), ObjectId.of(oid));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid AOR: " + s, e);
        }
    }

    public String protocol() { return protocol; }
    public String host() { return host; }
    public int port() { return port; }
    public ObjectId objectId() { return objectId; }

    @Override public String toString() {
        return protocol + "://" + host + ":" + port + "/" + objectId.value();
    }

    @Override public boolean equals(Object o) {
        return o instanceof AbsoluteObjectReference a
                && a.protocol.equals(protocol) && a.host.equals(host)
                && a.port == port && a.objectId.equals(objectId);
    }
    @Override public int hashCode() { return Objects.hash(protocol, host, port, objectId); }
}
