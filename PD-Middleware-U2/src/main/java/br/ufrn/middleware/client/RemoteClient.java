package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;

import java.util.Map;

/**
 * Client-side counterpart of {@link br.ufrn.middleware.protocol.ProtocolPlugin}.
 * Sends a request to a remote host and returns the raw body of the response.
 * Implementations exist per protocol (TCP HTTP, UDP).
 */
public interface RemoteClient {
    String protocol();

    String call(String host, int port, HttpMethod method, String path,
                Map<String, String> headers, String body);
}
