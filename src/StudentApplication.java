import java.util.LinkedHashMap;
import java.util.Map;
public final class StudentApplication {
    // Raw fixture storage only. Replace or wrap it with your protected domain objects.
    private final Map<String,Object> booking=new LinkedHashMap<>(Fixture.booking());
    public Map<String,Object> bookingSnapshot() { return Map.copyOf(booking); }
    public ProposalView propose(String targetId) {
        // TODO D1: validate target and baseline eligibility, generate an ID, capture
        // versions, create PENDING proposal. Never move the booking here.
        // This starter API reports a rejected target using IllegalArgumentException.
        throw new UnsupportedOperationException("D1 propose path");
    }
    public String run(ModelProvider provider,int limit) {
        // TODO D2: validate protocol, dispatch tools, retain observation history,
        // bound invocations and stop at pending approval. No automatic commit.
        // Add separate operator approve/reject/execute operations and tick(time).
        throw new UnsupportedOperationException("D2 model/tool coordination");
    }
    // TODO D3: evolve policy, preserve regressions, refactor and specify contracts.
}
