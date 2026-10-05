import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StudentApplication {
    private final Booking booking;
    private final Map<String, Space> spaces = new LinkedHashMap<>();
    private final List<Proposal> proposals = new ArrayList<>();
    private final EligibilityPolicy policy = new EligibilityPolicy();
    private int nextProposalNumber = 1;

    // Builds the domain objects from the supplied fixture; each application has its own copies.
    public StudentApplication() {
        booking = bookingFrom(Fixture.booking());
        for (Map<String, Object> raw : Fixture.spaces()) {
            Space space = spaceFrom(raw);
            if (spaces.put(space.getId(), space) != null) {
                throw new IllegalStateException("duplicate space in fixture: " + space.getId());
            }
        }
    }

    // Read-only copy of the booking; keys match the supplied fixture, in a stable order.
    public Map<String, Object> bookingSnapshot() {
        Map<String, Object> copy = new LinkedHashMap<>();
        copy.put("id", booking.getId());
        copy.put("spaceId", booking.getSpaceId());
        copy.put("durationHours", booking.getDurationHours());
        copy.put("version", booking.getVersion());
        copy.put("requiresAccessible", booking.requiresAccessible());
        return Collections.unmodifiableMap(copy);
    }

    // Read-only copy of one space, so callers can observe occupancy without changing it.
    public Map<String, Object> spaceSnapshot(String spaceId) {
        Space space = spaces.get(spaceId);
        if (space == null) {
            throw new IllegalArgumentException("Unknown space: " + spaceId);
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        copy.put("id", space.getId());
        copy.put("open", space.isOpen());
        copy.put("occupied", space.isOccupied());
        copy.put("accessible", space.isAccessible());
        return Collections.unmodifiableMap(copy);
    }

    // Unmodifiable views of every stored proposal, oldest first.
    public List<ProposalView> proposals() {
        return proposals.stream().map(Proposal::toView).toList();
    }

    // Creates and stores a PENDING proposal to move the booking to targetId.
    // Rejects malformed, unknown and ineligible targets with IllegalArgumentException.
    // Never moves the booking, changes occupancy, approves or executes.
    public ProposalView propose(String targetId) {
        DomainRules.requireIdentifier(targetId);
        Space target = spaces.get(targetId);
        if (target == null) {
            throw new IllegalArgumentException("Unknown target space: " + targetId);
        }
        if (!policy.isEligible(booking, target)) {
            throw new IllegalArgumentException("Target " + targetId
                    + " is not eligible under policy version " + policy.getVersion());
        }
        Proposal proposal = new Proposal(nextProposalId(), booking.getId(), targetId,
                                         booking.getVersion(), policy.getVersion());
        proposals.add(proposal);
        return proposal.toView();
    }

    public String run(ModelProvider provider,int limit) {
        // TODO D2: validate protocol, dispatch tools, retain observation history,
        // bound invocations and stop at pending approval. No automatic commit.
        // Add separate operator approve/reject/execute operations and tick(time).
        throw new UnsupportedOperationException("D2 model/tool coordination");
    }
    // TODO D3: evolve policy, preserve regressions, refactor and specify contracts.

    private String nextProposalId() {
        return "P" + nextProposalNumber++;
    }

    private static Booking bookingFrom(Map<String, Object> raw) {
        return new Booking((String) raw.get("id"), (String) raw.get("spaceId"),
                           (Integer) raw.get("durationHours"), (Integer) raw.get("version"),
                           (Boolean) raw.get("requiresAccessible"));
    }

    private static Space spaceFrom(Map<String, Object> raw) {
        return new Space((String) raw.get("id"), (Boolean) raw.get("open"),
                         (Boolean) raw.get("occupied"), (Boolean) raw.get("accessible"));
    }
}
