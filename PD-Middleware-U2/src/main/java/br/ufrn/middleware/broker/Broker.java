package br.ufrn.middleware.broker;

import br.ufrn.middleware.extension.InvocationInterceptor;
import br.ufrn.middleware.identification.Lookup;
import br.ufrn.middleware.identification.RemoteEntry;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.ProtocolRegistry;
import br.ufrn.middleware.protocol.tcp.TcpHttpProtocolPlugin;
import br.ufrn.middleware.protocol.udp.UdpProtocolPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Broker. Facade que une Lookup, Invoker, Marshaller, ProtocolRegistry, SRH. */
public final class Broker {
    private static final Logger log = LoggerFactory.getLogger(Broker.class);

    private final Lookup lookup;
    private final Marshaller marshaller;
    private final Invoker invoker;
    private final ProtocolRegistry protocols;
    private final List<InvocationInterceptor> globalInterceptors;
    private final String host;

    private final List<ProtocolPlugin> activePlugins = new ArrayList<>();

    private Broker(Builder b) {
        this.lookup = b.lookup;
        this.marshaller = b.marshaller;
        this.invoker = new Invoker(marshaller);
        this.protocols = b.protocols;
        this.globalInterceptors = List.copyOf(b.globalInterceptors);
        this.host = b.host;
    }

    public static Builder builder() { return new Builder(); }

    public Broker register(Class<?> remoteObjectClass) {
        RemoteEntry entry = RemoteObjectScanner.scan(remoteObjectClass);
        lookup.register(entry);
        log.info("Registered Remote Object id='{}' class={} lifecycle={} routes={}",
                entry.id(), remoteObjectClass.getSimpleName(),
                entry.instanceManager().kind(), entry.methods().size());
        return this;
    }

    public Broker register(Class<?>... classes) {
        for (Class<?> c : classes) register(c);
        return this;
    }

    /** Pode ser chamado N vezes para abrir multiplos protocolos. */
    public void start(String protocolName, int port) throws Exception {
        ProtocolPlugin plugin = protocols.get(protocolName);
        ServerRequestHandler srh = new ServerRequestHandler(
                lookup, invoker, marshaller, globalInterceptors,
                plugin.name(), host, port);
        plugin.start(port, srh::handle);
        activePlugins.add(plugin);
        log.info("Broker started: protocol={} host={} port={} objects={}",
                protocolName, host, port, lookup.size());
    }

    public void stop() {
        for (ProtocolPlugin p : activePlugins) {
            try { p.stop(); } catch (Exception ignored) {}
        }
        for (RemoteEntry e : lookup.all()) e.instanceManager().shutdown();
    }

    public Lookup lookup() { return lookup; }
    public Marshaller marshaller() { return marshaller; }

    public static final class Builder {
        private final Lookup lookup = new Lookup();
        private final Marshaller marshaller = new Marshaller();
        private final ProtocolRegistry protocols = new ProtocolRegistry();
        private final List<InvocationInterceptor> globalInterceptors = new ArrayList<>();
        private String host = "127.0.0.1";

        public Builder host(String h) { this.host = h; return this; }

        public Builder addProtocol(ProtocolPlugin p) { protocols.register(p); return this; }

        public Builder withDefaultProtocols() {
            return addProtocol(new TcpHttpProtocolPlugin())
                    .addProtocol(new UdpProtocolPlugin());
        }

        public Builder addGlobalInterceptor(InvocationInterceptor i) {
            globalInterceptors.add(i);
            return this;
        }

        public Broker build() { return new Broker(this); }
    }
}
