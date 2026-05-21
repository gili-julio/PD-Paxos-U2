package br.ufrn.kvstore.acceptor;

import br.ufrn.kvstore.common.PaxosAcceptRequest;
import br.ufrn.kvstore.common.PaxosAcceptedResponse;
import br.ufrn.kvstore.common.PaxosPrepareRequest;
import br.ufrn.kvstore.common.PaxosPromiseResponse;
import br.ufrn.middleware.annotation.Body;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.RemoteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Acceptor Paxos. STATIC. */
@RemoteObject(id = "acceptor")
@Lifecycle(Lifecycle.Kind.STATIC)
public class AcceptorService {
    private static final Logger log = LoggerFactory.getLogger(AcceptorService.class);

    @MethodMapping(method = HttpMethod.POST, path = "/prepare")
    public PaxosPromiseResponse prepare(@Body PaxosPrepareRequest req) {
        var ctx = AcceptorContext.get();
        var key = req.getKey();
        long n = req.getProposalNumber();
        var keyState = ctx.state().getOrCreate(key);

        synchronized (keyState) {
            if (n > keyState.highestPromisedProposal) {
                keyState.highestPromisedProposal = n;
                log.debug("[{}] PROMISE key={} n={}", ctx.acceptorId(), key, n);
                return PaxosPromiseResponse.promise(ctx.acceptorId(), n,
                        keyState.highestAcceptedProposal, keyState.acceptedValue);
            }
            log.debug("[{}] NACK fase1 key={} n={} < promised={}",
                    ctx.acceptorId(), key, n, keyState.highestPromisedProposal);
            return PaxosPromiseResponse.nack(ctx.acceptorId(), keyState.highestPromisedProposal);
        }
    }

    @MethodMapping(method = HttpMethod.POST, path = "/accept")
    public PaxosAcceptedResponse accept(@Body PaxosAcceptRequest req) {
        var ctx = AcceptorContext.get();
        var key = req.getKey();
        long n = req.getProposalNumber();
        String value = req.getValue();
        var keyState = ctx.state().getOrCreate(key);

        synchronized (keyState) {
            if (n >= keyState.highestPromisedProposal) {
                keyState.highestPromisedProposal = n;
                keyState.highestAcceptedProposal = n;
                keyState.acceptedValue = value;
                SharedKvStore.get().put(key, value);
                log.info("[{}] ACCEPTED key={} n={} value={}", ctx.acceptorId(), key, n, value);
                return PaxosAcceptedResponse.accepted(ctx.acceptorId(), n);
            }
            log.debug("[{}] NACK fase2 key={} n={} < promised={}",
                    ctx.acceptorId(), key, n, keyState.highestPromisedProposal);
            return PaxosAcceptedResponse.nack(ctx.acceptorId(), keyState.highestPromisedProposal);
        }
    }
}
