package br.ufrn.kvstore.common;

public class KvResult {
    private String key;
    private String value;
    private boolean found;

    public KvResult() {}
    public KvResult(String key, String value, boolean found) {
        this.key = key; this.value = value; this.found = found;
    }

    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
    public boolean isFound() { return found; }
    public void setFound(boolean f) { this.found = f; }
}
