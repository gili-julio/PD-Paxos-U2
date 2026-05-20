package br.ufrn.kvstore.common;

public class PutResponse {
    private boolean success;
    private String key;
    private String committedValue;
    private String error;

    public PutResponse() {}

    public static PutResponse ok(String key, String value) {
        var p = new PutResponse();
        p.success = true; p.key = key; p.committedValue = value;
        return p;
    }
    public static PutResponse fail(String key, String error) {
        var p = new PutResponse();
        p.success = false; p.key = key; p.error = error;
        return p;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean s) { this.success = s; }
    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    public String getCommittedValue() { return committedValue; }
    public void setCommittedValue(String v) { this.committedValue = v; }
    public String getError() { return error; }
    public void setError(String e) { this.error = e; }
}
