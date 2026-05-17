package br.ufrn.middleware.protocol;

import java.util.function.Function;

/**
 * Protocol Plug-In (Extension Pattern). Owns the wire format: starts a listener,
 * decodes bytes into {@link Request}s, hands them to the supplied dispatcher,
 * encodes the resulting {@link Response} and writes it back.
 */
public interface ProtocolPlugin {
    /** Identifier used in AORs and config (e.g. "tcp", "udp"). */
    String name();

    /** Start listening. {@code dispatcher} is the server-side processor (typically the SRH). */
    void start(int port, Function<Request, Response> dispatcher) throws Exception;

    /** Stop the listener and free resources. */
    void stop();
}
