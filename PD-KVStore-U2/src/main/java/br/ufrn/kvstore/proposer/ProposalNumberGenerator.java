package br.ufrn.kvstore.proposer;

import java.util.concurrent.atomic.AtomicLong;

/** Gerador monotonico de proposal numbers. */
public final class ProposalNumberGenerator {
    private final int proposerId;
    private final AtomicLong sequence = new AtomicLong(0);

    public ProposalNumberGenerator(int proposerPort) {
        this.proposerId = proposerPort % 1000;
    }

    public ProposalNumber next() {
        return new ProposalNumber(sequence.incrementAndGet(), proposerId);
    }

    public void updateFrom(long observedProposalNumber) {
        long observedSeq = observedProposalNumber / 1000;
        sequence.updateAndGet(current -> Math.max(current, observedSeq + 1));
    }
}
