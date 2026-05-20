package br.ufrn.kvstore.common;

import java.util.List;

public class PaxosRelayBatch {
    private int totalAcceptors;
    private List<String> responses;

    public PaxosRelayBatch() {}
    public PaxosRelayBatch(int totalAcceptors, List<String> responses) {
        this.totalAcceptors = totalAcceptors; this.responses = responses;
    }

    public int getTotalAcceptors() { return totalAcceptors; }
    public void setTotalAcceptors(int n) { this.totalAcceptors = n; }
    public List<String> getResponses() { return responses; }
    public void setResponses(List<String> r) { this.responses = r; }
}
