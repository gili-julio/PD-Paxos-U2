package br.ufrn.kvstore.common;

public class PaxosPromiseResponse {
    public static final String TYPE_PROMISE = "PROMISE";
    public static final String TYPE_NACK = "NACK";

    private String type;
    private String acceptorId;
    private long proposalNumber;
    private long acceptedProposalNumber;
    private String acceptedValue;

    public PaxosPromiseResponse() {}

    public static PaxosPromiseResponse promise(String acceptorId, long n, long acceptedN, String value) {
        var p = new PaxosPromiseResponse();
        p.type = TYPE_PROMISE; p.acceptorId = acceptorId; p.proposalNumber = n;
        p.acceptedProposalNumber = acceptedN; p.acceptedValue = value;
        return p;
    }
    public static PaxosPromiseResponse nack(String acceptorId, long n) {
        var p = new PaxosPromiseResponse();
        p.type = TYPE_NACK; p.acceptorId = acceptorId; p.proposalNumber = n;
        return p;
    }

    public boolean isPromise() { return TYPE_PROMISE.equals(type); }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAcceptorId() { return acceptorId; }
    public void setAcceptorId(String s) { this.acceptorId = s; }
    public long getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(long n) { this.proposalNumber = n; }
    public long getAcceptedProposalNumber() { return acceptedProposalNumber; }
    public void setAcceptedProposalNumber(long n) { this.acceptedProposalNumber = n; }
    public String getAcceptedValue() { return acceptedValue; }
    public void setAcceptedValue(String v) { this.acceptedValue = v; }
}
