package br.ufrn.kvstore.common;

import java.util.Objects;

public class ServiceInfo {
    private String id;          // proposer-9001, acceptor-10001
    private String role;        // proposer, acceptor
    private String host;
    private int port;
    private String protocol;    // tcp, udp
    private long lastHeartbeatMillis;

    public ServiceInfo() {}
    public ServiceInfo(String id, String role, String host, int port, String protocol) {
        this.id = id; this.role = role; this.host = host; this.port = port; this.protocol = protocol;
        this.lastHeartbeatMillis = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String r) { this.role = r; }
    public String getHost() { return host; }
    public void setHost(String h) { this.host = h; }
    public int getPort() { return port; }
    public void setPort(int p) { this.port = p; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String s) { this.protocol = s; }
    public long getLastHeartbeatMillis() { return lastHeartbeatMillis; }
    public void setLastHeartbeatMillis(long m) { this.lastHeartbeatMillis = m; }

    public String endpoint() { return host + ":" + port; }

    @Override public boolean equals(Object o) {
        return o instanceof ServiceInfo s && Objects.equals(s.id, id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
