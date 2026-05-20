package br.ufrn.kvstore.common;

public class PaxosPrepareRequest {
    private String key;
    private long proposalNumber;

    public PaxosPrepareRequest() {}
    public PaxosPrepareRequest(String key, long proposalNumber) {
        this.key = key;
        this.proposalNumber = proposalNumber;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public long getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(long proposalNumber) { this.proposalNumber = proposalNumber; }
}
