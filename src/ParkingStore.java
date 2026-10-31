import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// In-memory storage for one application: the booking, the spaces, the proposals, and the
// commit and notification records. It stores and finds; the rules live in the domain classes.
final class ParkingStore {
    private Booking booking;
    private final Map<String, Space> spaces = new LinkedHashMap<>();
    private final List<Proposal> proposals = new ArrayList<>();
    private final List<CommitRecord> commits = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();

    ParkingStore(Booking booking, List<Space> initialSpaces) {
        this.booking = booking;
        for (Space space : initialSpaces) {
            if (spaces.put(space.getId(), space) != null) {
                throw new IllegalStateException("duplicate space: " + space.getId());
            }
        }
    }

    Booking booking() {
        return booking;
    }

    // Booking is read-only, so a change is stored by swapping in the new copy
    void replaceBooking(Booking changed) {
        if (!changed.getId().equals(booking.getId())) {
            throw new IllegalArgumentException("not the stored booking: " + changed.getId());
        }
        booking = changed;
    }

    // The space with this id, or null when there is none
    Space space(String spaceId) {
        return spaces.get(spaceId);
    }

    Collection<Space> spaces() {
        return Collections.unmodifiableCollection(spaces.values());
    }

    Set<String> spaceIds() {
        return Set.copyOf(spaces.keySet());
    }

    void addProposal(Proposal proposal) {
        proposals.add(proposal);
    }

    // The proposal with this id, or null when there is none
    Proposal proposal(String proposalId) {
        for (Proposal proposal : proposals) {
            if (proposal.getProposalId().equals(proposalId)) {
                return proposal;
            }
        }
        return null;
    }

    List<Proposal> proposals() {
        return Collections.unmodifiableList(proposals);
    }

    void recordCommit(CommitRecord commit) {
        commits.add(commit);
    }

    int commitCount() {
        return commits.size();
    }

    void addNotification(Notification notification) {
        notifications.add(notification);
    }

    List<Notification> notifications() {
        return List.copyOf(notifications);
    }
}
