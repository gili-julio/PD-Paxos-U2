package br.ufrn.kvstore.common;

public class PaxosAcceptRequest {
    private String key;
    private long proposalNumber;
    private String value;

    public PaxosAcceptRequest() {}
    public PaxosAcceptRequest(String key, long proposalNumber, String value) {
        this.key = key; this.proposalNumber = proposalNumber; this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    public long getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(long n) { this.proposalNumber = n; }
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
}
