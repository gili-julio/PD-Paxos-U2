package br.ufrn.middleware.protocol;

import java.util.function.Function;

/** Protocol Plug-In. Listener que decodifica wire e chama dispatcher. */
public interface ProtocolPlugin {
    String name();
    void start(int port, Function<Request, Response> dispatcher) throws Exception;
    void stop();
}
