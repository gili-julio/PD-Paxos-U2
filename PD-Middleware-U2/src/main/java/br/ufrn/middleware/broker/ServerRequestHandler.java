package br.ufrn.middleware.broker;

import br.ufrn.middleware.extension.InvocationInterceptor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.identification.Lookup;
import br.ufrn.middleware.identification.ObjectId;
import br.ufrn.middleware.identification.RemoteEntry;
import br.ufrn.middleware.protocol.Request;
import br.ufrn.middleware.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** Server Request Handler. URL = /<objectId>/<methodPath>. */
public final class ServerRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ServerRequestHandler.class);

    private final Lookup lookup;
    private final Invoker invoker;
    private final Marshaller marshaller;
    private final List<InvocationInterceptor> globalInterceptors;
    private final String localProtocol;
    private final String localHost;
    private final int localPort;

    public ServerRequestHandler(Lookup lookup, Invoker invoker, Marshaller marshaller,
                                List<InvocationInterceptor> globalInterceptors,
                                String localProtocol, String localHost, int localPort) {
        this.lookup = lookup;
        this.invoker = invoker;
        this.marshaller = marshaller;
        this.globalInterceptors = List.copyOf(globalInterceptors);
        this.localProtocol = localProtocol;
        this.localHost = localHost;
        this.localPort = localPort;
    }

    public Response handle(Request request) {
        try {
            String[] parts = splitFirstSegment(request.path());
            ObjectId oid = ObjectId.of(parts[0]);
            String rest = parts[1];

            RemoteEntry entry = lookup.find(oid).orElseThrow(
                    () -> RemotingException.notFound("Unknown Remote Object: " + oid));

            for (RemoteMethod m : entry.methods()) {
                if (m.httpMethod() != request.method()) continue;
                Map<String, String> vars = m.path().match(rest);
                if (vars == null) continue;
                AbsoluteObjectReference aor = new AbsoluteObjectReference(
                        localProtocol, localHost, localPort, oid);
                Object result = invoker.invoke(entry, m, vars, request, aor, globalInterceptors);
                return resultToResponse(result);
            }
            throw RemotingException.notFound("No route matches " + request.method() + " " + request.path());
        } catch (RemotingException re) {
            log.warn("Remoting error: [{}] {}", re.httpStatus(), re.getMessage());
            return errorResponse(re.httpStatus(), re.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error while processing {} {}", request.method(), request.path(), e);
            return errorResponse(500, "Internal error: " + e.getMessage());
        }
    }

    private static String[] splitFirstSegment(String path) {
        if (path == null || path.isBlank()) throw RemotingException.badRequest("Empty path");
        String p = path.startsWith("/") ? path.substring(1) : path;
        int slash = p.indexOf('/');
        if (slash < 0) return new String[] { p, "" };
        return new String[] { p.substring(0, slash), p.substring(slash) };
    }

    private Response resultToResponse(Object result) {
        if (result == null) return new Response(204, Map.of(), "");
        if (result instanceof Response r) return r;
        if (result instanceof CharSequence cs) return Response.text(200, cs.toString());
        return Response.json(200, marshaller.toJson(result));
    }

    private Response errorResponse(int status, String message) {
        String json = marshaller.toJson(Map.of("error", message));
        return Response.json(status, json);
    }
}
