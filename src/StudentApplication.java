import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StudentApplication {
    private final ParkingStore store;
    private final EligibilityPolicy policy = new EligibilityPolicy();
    private final PeriodicChecker checker;
    private int nextProposalNumber = 1;

    // Builds the domain objects from the supplied fixture; each application has its own copies.
    public StudentApplication() {
        store = new ParkingStore(bookingFrom(Fixture.booking()),
                                 Fixture.spaces().stream().map(StudentApplication::spaceFrom).toList());
        checker = new PeriodicChecker(store, policy);
    }

    // Read-only copy of the booking; keys match the supplied fixture, in a stable order.
    public Map<String, Object> bookingSnapshot() {
        Booking booking = store.booking();
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
        Space space = requireSpace(spaceId);
        Map<String, Object> copy = new LinkedHashMap<>();
        copy.put("id", space.getId());
        copy.put("open", space.isOpen());
        copy.put("occupied", space.isOccupied());
        copy.put("accessible", space.isAccessible());
        return Collections.unmodifiableMap(copy);
    }

    // Unmodifiable views of every stored proposal, oldest first.
    public List<ProposalView> proposals() {
        return store.proposals().stream().map(Proposal::toView).toList();
    }

    // Creates and stores a PENDING proposal to move the booking to targetId.
    // Rejects malformed, unknown and ineligible targets with IllegalArgumentException.
    // Never moves the booking, changes occupancy, approves or executes.
    public ProposalView propose(String targetId) {
        DomainRules.requireIdentifier(targetId);
        Booking booking = store.booking();
        Space target = store.space(targetId);
        if (target == null) {
            throw new IllegalArgumentException("Unknown target space: " + targetId);
        }
        if (!policy.isEligible(booking, target)) {
            throw new IllegalArgumentException("Target " + targetId
                    + " is not eligible under policy version " + policy.getVersion());
        }
        Proposal proposal = new Proposal(nextProposalId(), booking.getId(), targetId,
                                         booking.getVersion(), policy.getVersion());
        store.addProposal(proposal);
        return proposal.toView();
    }

    // Runs the model/tool loop with at most `limit` model calls. It can create one PENDING proposal
    // and then stops; approval and execution are separate operator actions.
    public AgentResult run(ModelProvider provider, int limit) {
        return new AgentRunner(this, provider, limit).run();
    }

    // The policy version proposals are checked against
    public int policyVersion() {
        return policy.getVersion();
    }

    // Ids of the spaces the booking could move to right now, in fixture order
    public List<String> alternatives() {
        Booking booking = store.booking();
        return store.spaces().stream()
                     .filter(space -> policy.isEligible(booking, space))
                     .map(Space::getId)
                     .toList();
    }

    // Ids of every known space
    public Set<String> spaceIds() {
        return store.spaceIds();
    }

    // Operator action: PENDING -> APPROVED. An unknown id is an IllegalArgumentException;
    // a proposal that is not PENDING is an IllegalStateException and stays as it was.
    public ProposalView approve(String proposalId) {
        Proposal proposal = requireProposal(proposalId);
        proposal.approve();
        return proposal.toView();
    }

    // Operator action: PENDING -> REJECTED, with the same refusals as approve
    public ProposalView reject(String proposalId) {
        Proposal proposal = requireProposal(proposalId);
        proposal.reject();
        return proposal.toView();
    }

    // Operator action: executes an approved proposal after checking the current state again.
    // Checks run in this order and the first failure is returned with nothing changed:
    // unknown proposal (IllegalArgumentException), already executed (DUPLICATE), not for this booking
    // (IllegalArgumentException), not approved
    // (NOT_APPROVED), booking or policy version changed (STALE), target not eligible now
    // (UNAVAILABLE_OR_INELIGIBLE). Validation and mutation run with no model or external call between.
    public ExecutionOutcome execute(String proposalId) {
        Proposal proposal = requireProposal(proposalId);
        if (proposal.getStatus() == ProposalStatus.EXECUTED) {
            return ExecutionOutcome.DUPLICATE;   // recognised first: a committed proposal never commits again
        }
        Booking booking = store.booking();
        if (!proposal.getBookingId().equals(booking.getId())) {
            throw new IllegalArgumentException("Proposal " + proposalId + " is not for booking " + booking.getId());
        }
        if (proposal.getStatus() != ProposalStatus.APPROVED) {
            return ExecutionOutcome.NOT_APPROVED;
        }
        if (proposal.getObservedBookingVersion() != booking.getVersion()
                || proposal.getObservedPolicyVersion() != policy.getVersion()) {
            return ExecutionOutcome.STALE;
        }
        Space target = store.space(proposal.getTargetId());
        if (target == null || !policy.isEligible(booking, target)) {
            return ExecutionOutcome.UNAVAILABLE_OR_INELIGIBLE;
        }
        commit(proposal, booking, target);
        return ExecutionOutcome.COMMITTED;
    }

    // Changes the booking's duration (1..24); the booking's version goes up by one and it does not move
    public void changeBookingDuration(int hours) {
        store.replaceBooking(store.booking().withDuration(hours));
    }

    // Closes a space (it stops being available)
    public void closeSpace(String spaceId) {
        requireSpace(spaceId).close();
    }

    // Marks a space occupied; an already occupied space is an IllegalStateException
    public void occupySpace(String spaceId) {
        requireSpace(spaceId).occupy();
    }

    // Periodic check at an explicit time in seconds. The first tick scans; later ticks scan only at
    // least 60 seconds after the last scan. A time below the previous tick is an IllegalArgumentException.
    // Returns the new work items; it never proposes, approves or moves anything.
    public List<WorkItem> tick(long seconds) {
        return checker.tick(seconds);
    }

    // Operator action: allow the next scan to emit work for this booking again
    public void retry(String bookingId) {
        checker.retry(bookingId);
    }

    public int commitCount() {
        return store.commitCount();
    }

    // Copies of the simulated notifications, oldest first
    public List<Notification> notifications() {
        return store.notifications();
    }

    // TODO D3: evolve policy, preserve regressions, refactor and specify contracts.

    // The one place a booking moves: free the old space, take the target, move the booking
    // (version + 1), mark the proposal executed, and record one commit and one notification
    private void commit(Proposal proposal, Booking booking, Space target) {
        Space oldSpace = store.space(booking.getSpaceId());
        target.occupy();
        if (oldSpace != null) {
            oldSpace.release();
        }
        Booking moved = booking.movedTo(target.getId());
        store.replaceBooking(moved);
        proposal.markExecuted();
        store.recordCommit(new CommitRecord(proposal.getProposalId(), moved.getId(),
                                            booking.getSpaceId(), moved.getSpaceId(), moved.getVersion()));
        store.addNotification(new Notification(moved.getId(), "Booking " + moved.getId() + " moved from "
                + booking.getSpaceId() + " to " + moved.getSpaceId() + " (proposal " + proposal.getProposalId() + ")"));
    }

    private Proposal requireProposal(String proposalId) {
        Proposal proposal = store.proposal(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("Unknown proposal: " + proposalId);
        }
        return proposal;
    }

    private Space requireSpace(String spaceId) {
        Space space = store.space(spaceId);
        if (space == null) {
            throw new IllegalArgumentException("Unknown space: " + spaceId);
        }
        return space;
    }

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
