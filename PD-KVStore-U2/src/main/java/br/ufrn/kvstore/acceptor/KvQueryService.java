package br.ufrn.kvstore.acceptor;

import br.ufrn.kvstore.common.KvResult;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.PathVar;
import br.ufrn.middleware.annotation.RemoteObject;

/** Leitura do KV. LEASED: instancia expira; SharedKvStore preserva dados. */
@RemoteObject(id = "kv")
@Lifecycle(value = Lifecycle.Kind.LEASED, ttlSeconds = 60)
public class KvQueryService {

    @MethodMapping(method = HttpMethod.GET, path = "/entries/{key}")
    public KvResult get(@PathVar("key") String key) {
        var v = SharedKvStore.get().get(key);
        return new KvResult(key, v.orElse(null), v.isPresent());
    }

    @MethodMapping(method = HttpMethod.GET, path = "/size")
    public int size() { return SharedKvStore.get().size(); }
}
