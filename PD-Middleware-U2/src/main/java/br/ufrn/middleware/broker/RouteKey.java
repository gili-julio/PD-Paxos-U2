package br.ufrn.middleware.broker;

import br.ufrn.middleware.annotation.HttpMethod;

import java.util.Objects;

/** Chave da tabela de rotas. */
public record RouteKey(HttpMethod method, String pathTemplate) {
    public RouteKey {
        Objects.requireNonNull(method);
        Objects.requireNonNull(pathTemplate);
    }
}
