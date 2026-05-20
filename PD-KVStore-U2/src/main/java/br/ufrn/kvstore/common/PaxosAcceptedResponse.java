package br.ufrn.kvstore.common;

public class PaxosAcceptedResponse {
    public static final String TYPE_ACCEPTED = "ACCEPTED";
    public static final String TYPE_NACK = "NACK";

    private String type;
    private String acceptorId;
    private long proposalNumber;

    public PaxosAcceptedResponse() {}

    public static PaxosAcceptedResponse accepted(String acceptorId, long n) {
        var r = new PaxosAcceptedResponse();
        r.type = TYPE_ACCEPTED; r.acceptorId = acceptorId; r.proposalNumber = n;
        return r;
    }
    public static PaxosAcceptedResponse nack(String acceptorId, long n) {
        var r = new PaxosAcceptedResponse();
        r.type = TYPE_NACK; r.acceptorId = acceptorId; r.proposalNumber = n;
        return r;
    }

    public boolean isAccepted() { return TYPE_ACCEPTED.equals(type); }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public String getAcceptorId() { return acceptorId; }
    public void setAcceptorId(String s) { this.acceptorId = s; }
    public long getProposalNumber() { return proposalNumber; }
    public void setProposalNumber(long n) { this.proposalNumber = n; }
}
