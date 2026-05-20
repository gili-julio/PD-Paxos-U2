package br.ufrn.kvstore.proposer;

public final class ProposalNumber {
    private static final int MAX_PROPOSERS = 1000;
    private final long sequence;
    private final int proposerId;
    private final long value;

    public ProposalNumber(long sequence, int proposerId) {
        this.sequence = sequence;
        this.proposerId = proposerId;
        this.value = sequence * MAX_PROPOSERS + proposerId;
    }

    public long value() { return value; }
    public long sequence() { return sequence; }
    public int proposerId() { return proposerId; }
}
