package br.ufrn.middleware.protocol;

import java.util.HashMap;
import java.util.Map;

/** Registry nome -> ProtocolPlugin. */
public final class ProtocolRegistry {
    private final Map<String, ProtocolPlugin> plugins = new HashMap<>();

    public ProtocolRegistry register(ProtocolPlugin plugin) {
        plugins.put(plugin.name().toLowerCase(), plugin);
        return this;
    }

    public ProtocolPlugin get(String name) {
        ProtocolPlugin p = plugins.get(name.toLowerCase());
        if (p == null) throw new IllegalArgumentException(
                "No ProtocolPlugin registered for '" + name + "'. Available: " + plugins.keySet());
        return p;
    }
}
