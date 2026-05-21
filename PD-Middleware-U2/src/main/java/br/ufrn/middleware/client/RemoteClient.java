package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;

import java.util.Map;

/** Cliente do protocolo. Espelho do ProtocolPlugin do lado servidor. */
public interface RemoteClient {
    String protocol();

    String call(String host, int port, HttpMethod method, String path,
                Map<String, String> headers, String body);
}
