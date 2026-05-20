# PD-Middleware-U2

Plataforma de middleware Java implementando os **Remoting Patterns** (Voelter, Kircher, Jain – *Remoting Patterns: Foundations of Enterprise, Internet and Realtime Distributed Object Middleware*) para a disciplina **Programação Distribuída** (UFRN/IMD), Unidade 2.

A plataforma é distribuída como um JAR e consumida pelo projeto `PD-KVStore-U2`, que reimplementa o KV Store com consenso Paxos da Unidade 1 sobre esta middleware.

## Visão geral

```
JMeter ──HTTP──> Aplicação (anotada com @RemoteObject)
                       │
                       │ usa via JAR
                       ▼
                 PD-Middleware-U2
                 (Broker + SRH + Invoker + Marshaller +
                  Lookup + Lifecycle + Interceptors + Plug-Ins)
```

## Mapa Padrões → Classes

| Padrão | Classe |
|---|---|
| **Broker** | `broker/Broker.java` |
| **Server Request Handler** | `broker/ServerRequestHandler.java` |
| **Invoker** | `broker/Invoker.java` |
| **Marshaller** | `broker/Marshaller.java` |
| **Remote Object** | anotação `annotation/RemoteObject.java` |
| **Remoting Error** | `broker/RemotingException.java` |
| **Object Id** | `identification/ObjectId.java` |
| **Absolute Object Reference** | `identification/AbsoluteObjectReference.java` |
| **Lookup** | `identification/Lookup.java` |
| **Static Instance** | `lifecycle/StaticInstanceManager.java` |
| **Per-Request Instance** | `lifecycle/PerRequestInstanceManager.java` |
| **Pooling** | `lifecycle/PooledInstanceManager.java` |
| **Leasing** | `lifecycle/LeasedInstanceManager.java` |
| **Invocation Interceptor** | `extension/InvocationInterceptor.java` + `InterceptorChain.java` |
| **Invocation Context** | `extension/InvocationContext.java` |
| **Protocol Plug-In** | `protocol/ProtocolPlugin.java` + `protocol/tcp/`, `protocol/udp/` |

## Modelo de Componente

Métodos remotos são definidos por anotações que **preservam a assinatura Java** (parâmetros tipados, sem `JSONObject` cru):

```java
@RemoteObject(id = "kv")
@Lifecycle(value = Lifecycle.Kind.LEASED, ttlSeconds = 60)
public class KvQueryService {

    @MethodMapping(method = HttpMethod.GET, path = "/entries/{key}")
    public KvResult get(@PathVar("key") String key) { ... }

    @MethodMapping(method = HttpMethod.POST, path = "/insert")
    public PutResponse put(@Body PutRequest req,
                           @Header("X-Trace-Id") String trace,
                           InvocationContext ctx) { ... }
}
```

Bindings suportados: `@PathVar`, `@Param` (corpo JSON), `@Body` (corpo inteiro), `@Header`, `InvocationContext`.

## Como compilar

```bash
mvn clean install
```

Gera `target/pd-middleware-1.0-SNAPSHOT.jar` e instala no repositório Maven local (`~/.m2`) com coordenadas `br.ufrn.imd.pd:pd-middleware:1.0-SNAPSHOT`.

## Uso na aplicação

```java
Broker broker = Broker.builder()
        .host("127.0.0.1")
        .withDefaultProtocols()                 // registra TCP + UDP
        .addGlobalInterceptor(new LoggingInterceptor())
        .build();

broker.register(MyService.class);               // scaneia anotações
broker.start("tcp", 8080);
```
