package br.ufrn.kvstore.proposer;

import br.ufrn.kvstore.common.PaxosAcceptRequest;
import br.ufrn.kvstore.common.PaxosAcceptedResponse;
import br.ufrn.kvstore.common.PaxosPrepareRequest;
import br.ufrn.kvstore.common.PaxosPromiseResponse;
import br.ufrn.kvstore.common.PaxosRelayBatch;
import br.ufrn.kvstore.common.PutResponse;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.PathVar;
import br.ufrn.middleware.annotation.RemoteObject;
import br.ufrn.middleware.annotation.Body;
import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.client.RemoteCallException;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Proposer Remote Object. Per-Request lifecycle: every {@code PUT} produces a
 * fresh instance, so concurrent rounds never share mutable state. The shared
 * {@link ProposalNumberGenerator} and {@link ClientBroker} live in
 * {@link ProposerContext}.
 *
 * <p>The Proposer drives 2-phase Paxos by sending {@code prepare}/{@code accept}
 * to the Gateway's relay service, which fans out to all known acceptors.
 */
@RemoteObject(id = "proposer")
@Lifecycle(Lifecycle.Kind.PER_REQUEST)
public class ProposerService {
    private static final Logger log = LoggerFactory.getLogger(ProposerService.class);

    private static final int MAX_RETRIES = 10;
    private static final long RETRY_MIN_MS = 100;
    private static final long RETRY_MAX_MS = 1000;

    private final Gson gson = new Gson();

    public ProposerService() {}

    @MethodMapping(method = HttpMethod.PUT, path = "/entries/{key}")
    public PutResponse put(@PathVar("key") String key, @Body String rawBody) {
        String value = unwrapBody(rawBody);
        if (value == null || value.isEmpty()) {
            return PutResponse.fail(key, "Empty body");
        }
        return runPaxos(key, value);
    }

    private String unwrapBody(String body) {
        if (body == null) return null;
        String trimmed = body.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private PutResponse runPaxos(String key, String value) {
        var ctx = ProposerContext.get();
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            log.info("[PAXOS] attempt {}/{} key='{}'", attempt, MAX_RETRIES, key);
            ProposalNumber n = ctx.generator().next();

            List<PaxosPromiseResponse> promises = sendPrepare(ctx.clientBroker(), key, n);
            if (promises == null) { backoff(attempt); continue; }

            long promised = promises.stream().filter(PaxosPromiseResponse::isPromise).count();
            long needed = (promises.size() / 2) + 1;
            log.info("[PAXOS] phase1 {}/{} promises (quorum={})", promised, promises.size(), needed);
            if (promised < needed) {
                promises.stream().filter(p -> !p.isPromise())
                        .mapToLong(PaxosPromiseResponse::getProposalNumber).max()
                        .ifPresent(ctx.generator()::updateFrom);
                backoff(attempt);
                continue;
            }

            String valueToPropose = chooseSafeValue(promises, value);
            if (!valueToPropose.equals(value)) {
                log.info("[PAXOS] using previously accepted value '{}'", valueToPropose);
            }

            List<PaxosAcceptedResponse> accepteds = sendAccept(ctx.clientBroker(), key, n.value(), valueToPropose);
            if (accepteds == null) { backoff(attempt); continue; }

            long accepted = accepteds.stream().filter(PaxosAcceptedResponse::isAccepted).count();
            long neededAcc = (accepteds.size() / 2) + 1;
            log.info("[PAXOS] phase2 {}/{} accepted (quorum={})", accepted, accepteds.size(), neededAcc);

            if (accepted >= neededAcc) {
                log.info("[PAXOS] consensus achieved key='{}' value='{}'", key, valueToPropose);
                return PutResponse.ok(key, valueToPropose);
            }
            backoff(attempt);
        }
        return PutResponse.fail(key, "Consensus not reached after " + MAX_RETRIES + " attempts");
    }

    private List<PaxosPromiseResponse> sendPrepare(ClientBroker cb, String key, ProposalNumber n) {
        var ctx = ProposerContext.get();
        try {
            String body = gson.toJson(new PaxosPrepareRequest(key, n.value()));
            String resp = cb.callRaw(ctx.gatewayRelay(), HttpMethod.POST, "/prepare",
                    Map.of("X-Paxos-Action", "PREPARE"), body);
            PaxosRelayBatch batch = gson.fromJson(resp, PaxosRelayBatch.class);
            return parsePromises(batch);
        } catch (RemoteCallException e) {
            log.error("[PAXOS] PREPARE relay failed: {}", e.getMessage());
            return null;
        }
    }

    private List<PaxosAcceptedResponse> sendAccept(ClientBroker cb, String key, long n, String value) {
        var ctx = ProposerContext.get();
        try {
            String body = gson.toJson(new PaxosAcceptRequest(key, n, value));
            String resp = cb.callRaw(ctx.gatewayRelay(), HttpMethod.POST, "/accept",
                    Map.of("X-Paxos-Action", "ACCEPT"), body);
            PaxosRelayBatch batch = gson.fromJson(resp, PaxosRelayBatch.class);
            return parseAccepteds(batch);
        } catch (RemoteCallException e) {
            log.error("[PAXOS] ACCEPT relay failed: {}", e.getMessage());
            return null;
        }
    }

    private List<PaxosPromiseResponse> parsePromises(PaxosRelayBatch batch) {
        var out = new ArrayList<PaxosPromiseResponse>();
        if (batch == null || batch.getResponses() == null) return out;
        for (String json : batch.getResponses()) {
            try { out.add(gson.fromJson(json, PaxosPromiseResponse.class)); }
            catch (Exception e) { log.warn("Cannot parse Promise: {}", json); }
        }
        return out;
    }

    private List<PaxosAcceptedResponse> parseAccepteds(PaxosRelayBatch batch) {
        var out = new ArrayList<PaxosAcceptedResponse>();
        if (batch == null || batch.getResponses() == null) return out;
        for (String json : batch.getResponses()) {
            try { out.add(gson.fromJson(json, PaxosAcceptedResponse.class)); }
            catch (Exception e) { log.warn("Cannot parse Accepted: {}", json); }
        }
        return out;
    }

    private String chooseSafeValue(List<PaxosPromiseResponse> promises, String clientValue) {
        return promises.stream()
                .filter(PaxosPromiseResponse::isPromise)
                .filter(p -> p.getAcceptedValue() != null && !p.getAcceptedValue().isBlank())
                .max((a, b) -> Long.compare(a.getAcceptedProposalNumber(), b.getAcceptedProposalNumber()))
                .map(PaxosPromiseResponse::getAcceptedValue)
                .orElse(clientValue);
    }

    private void backoff(int attempt) {
        if (attempt >= MAX_RETRIES) return;
        long delay = ThreadLocalRandom.current().nextLong(RETRY_MIN_MS, RETRY_MAX_MS + 1);
        try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
