package br.ufrn.middleware.broker;

import br.ufrn.middleware.annotation.HttpMethod;

import java.util.Objects;

/** (method, pathTemplate) key for the route table. */
public record RouteKey(HttpMethod method, String pathTemplate) {
    public RouteKey {
        Objects.requireNonNull(method);
        Objects.requireNonNull(pathTemplate);
    }
}
