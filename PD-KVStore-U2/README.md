# PD-KVStore-U2

Aplicação distribuída — KV Store com consenso Paxos — implementada sobre a plataforma `PD-Middleware-U2`. Trabalho da Unidade 2 de **Programação Distribuída** (UFRN/IMD).

A lógica Paxos é a mesma da Unidade 1 (`PD-Paxos-U1`), mas agora exposta via **anotações** da middleware. Nenhum código de protocolo (TCP/UDP) aparece nas classes de negócio — a troca é feita por linha de comando.

## Arquitetura

Toda comunicação externa passa pelo **Gateway** — JMeter (ou qualquer cliente) só conhece a porta do Gateway. Proposers e Acceptors ficam atrás dele.

O Gateway escuta **dois protocolos simultâneos**: TCP (porta 8088) para tráfego e UDP (porta 8089) dedicado a heartbeat — separa o canal de controle do canal de carga para que rajadas TCP não atrasem heartbeats.

```
JMeter ── PUT /gateway/kv/{key} ───▶  Gateway TCP (8088)
                                          │
                                          │ roteia (round-robin)
                                          ▼
                                  Proposer  ──── consenso ────┐
                                          ▲                    │
                                          │ relay              │
                                          │ PREPARE/ACCEPT     │
                                          ▼                    │
                                  Gateway/PaxosRelayWorker     │
                                  (pool de 512)                │
                                          │ fanout             │
                                          ▼                    │
                                  Acceptors A1/A2/A3 ◀─────────┘
                                          │ commit
                                          ▼
                                  SharedKvStore

JMeter ── GET /gateway/kv/{key} ───▶  Gateway ─▶ Acceptor (round-robin)

Proposer/Acceptor ── heartbeat UDP ─▶ Gateway UDP (8089) (a cada 2 s)
```

3 papéis (`--role=gateway|proposer|acceptor`). Cada nó interno se registra no Gateway na partida e mantém *heartbeat* a cada 2 s — auto-reconecta se o Gateway cair, e re-registra se for marcado inativo (resposta `unknown`).

## Mapa Padrões da Middleware → @RemoteObject da app

| Classe (`@RemoteObject`) | Lifecycle | Por quê |
|---|---|---|
| `gateway/GatewayService` | **STATIC** | registry, heartbeat e roteamento são singleton |
| `gateway/PaxosRelayWorker` | **POOLED**(512) | pool limita rounds Paxos concorrentes — backpressure |
| `proposer/ProposerService` | **PER_REQUEST** | cada PUT spawn instância nova; estado mutável fica isolado |
| `acceptor/AcceptorService` | **STATIC** | estado de consenso por chave é único por nó |
| `acceptor/KvQueryService` | **LEASED**(60s) | fachada de leitura é descartada se ociosa; dado vive em `SharedKvStore` |

Todas as 4 estratégias de lifecycle suportadas pela middleware aparecem na aplicação.

## Como compilar

Primeiro instale a middleware no repositório Maven local:

```bash
cd ../PD-Middleware-U2
mvn -DskipTests clean install

cd ../PD-KVStore-U2
mvn -DskipTests clean package
```

Ou rode `build-all.bat` (Windows) da raiz da app.

## Como executar (TCP)

```bat
start-gateway-tcp.bat
start-acceptors-tcp.bat
start-proposers-tcp.bat
```

Trocar TCP por UDP: usar os scripts `*-udp.bat`. Aplicação não muda (heartbeat continua UDP de qualquer forma).

### Endpoints

**Públicos** (JMeter/cliente bate só no Gateway TCP):

| Método | URL                                          | O que faz |
|--------|----------------------------------------------|---|
| `PUT`  | `http://<gateway>/gateway/kv/{key}` body=`<valor>` | dispara consenso Paxos (rota para Proposer) |
| `GET`  | `http://<gateway>/gateway/kv/{key}`          | leitura (rota para Acceptor) |
| `GET`  | `http://<gateway>/gateway/services`          | lista nós ativos |

**Internos** (entre nós):

| Método | URL                                              | Origem | Canal |
|--------|--------------------------------------------------|---|---|
| `POST` | `http://<gateway>:8088/gateway/register`         | Proposer/Acceptor no boot | TCP |
| `POST` | `udp://<gateway>:8089/gateway/heartbeat`         | Proposer/Acceptor a cada 2 s | UDP |
| `POST` | `http://<gateway>/paxos-relay/prepare`           | Proposer durante consenso | TCP |
| `POST` | `http://<gateway>/paxos-relay/accept`            | Proposer durante consenso | TCP |
| `POST` | `http://<acceptor>/acceptor/prepare`             | PaxosRelayWorker (fanout) | TCP |
| `POST` | `http://<acceptor>/acceptor/accept`              | PaxosRelayWorker (fanout) | TCP |
| `GET`  | `http://<acceptor>/kv/entries/{key}`             | Gateway ao rotear leitura | TCP |
| `GET`  | `http://<acceptor>/kv/size`                      | utilitário | TCP |

### Smoke test

```bash
curl -X PUT -d "hello" http://localhost:8088/gateway/kv/foo

curl http://localhost:8088/gateway/kv/foo
```

## Resiliência e desempenho

A plataforma tem mecanismos para tolerar carga alta e falhas transitórias:

- **Keep-alive HTTP/1.1**: tanto o servidor (`TcpHttpProtocolPlugin`) quanto o cliente (`TcpHttpClient`) reusam conexões. Pool client-side de até 64 conexões por destino.
- **Heartbeat UDP separado**: heartbeat usa porta UDP dedicada (8089), não compete com tráfego TCP pelo mesmo pool de sockets.
- **Auto re-registro**: se o Gateway responde `{"status":"unknown"}` (nó foi marcado inativo por `HEARTBEAT_TIMEOUT_MS`), o nó re-registra automaticamente no próximo tick (2 s).
- **Retry de registro inicial**: se o Gateway estiver fora no boot do Proposer/Acceptor, o nó fica tentando registrar a cada 2 s até conseguir.
- **Pool de relay (512)**: limita rounds Paxos concorrentes para apresentação de backpressure.

## Testes de carga (JMeter)

Todos os planos batem no **Gateway** (porta 8088):

| Arquivo | Cenário | Parâmetro `-J` |
|---|---|---|
| `kv-put-load.jmx` | escrita pura → consenso Paxos | `threads=10` |
| `kv-get-load.jmx` | leitura pura → 1 acceptor (via gateway) | `threads=20` |
| `kv-capacity-ramp.jmx` | rampa até **knee/usable** | `threads=80` (rampa 120 s, duração 180 s) |

Recomendações no JMeter para reduzir erros sob carga:
- **Use KeepAlive** = on (HTTP Request Defaults) — reusa conexão TCP entre samples
- **Connect Timeout** = 5000 ms, **Response Timeout** = 30000 ms
- **Ramp-up** ≥ 60 s evita estourar backlog no boot
- Path único por sample: `/gateway/kv/k-${__threadNum}-${__counter(TRUE)}` evita contenção Paxos

Rodar headless:

```bash
jmeter -n -t jmeter/kv-capacity-ramp.jmx -Jthreads=80 -l results.jtl -e -o report/
```

### Análise de capacidade

- **Knee capacity** — número de usuários a partir do qual a curva **latência × throughput** deixa de ser linear; ponto onde a fila começa a se formar (sockets do cliente, pool de relay, threads dos acceptors).
- **Usable capacity** — carga máxima sustentável com latência aceitável (ex.: p95 < SLA). Sempre ≤ knee.

Para localizar:
1. Rampe `--threads` de 5 → 100 em incrementos.
2. Plote `throughput` (req/s) e `p95 latency` no `report/index.html`.
3. Knee = ponto de inflexão. Usable = maior carga ainda abaixo do SLA.

O `PaxosRelayWorker` está em `poolSize=512` — generoso para destacar throughput, mas ainda finito (acima de 512 rounds em vôo, requisições aguardam até 5 s pelo pool antes de retornar `500`). Diminuir para 4 reativa um knee bem visível para fins didáticos.
