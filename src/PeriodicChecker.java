import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Explicit 60-second ticks with time supplied by the caller (no sleeping, no system clock).
// The first tick scans; after that a scan happens on the first tick at or past each boundary of
// the 60-second grid that starts at the first tick (first + 60, first + 120, ...).
// Finds bookings on closed spaces and emits each booking/policy version pair at most once.
// Conservative retry: once emitted, a pair is not emitted again on its own, even if the attempt
// failed; the operator calls retry, or a version change makes a new pair.
final class PeriodicChecker {
    static final long PERIOD_SECONDS = 60;

    private final ParkingStore store;
    private final EligibilityPolicy policy;
    private final Set<String> emitted = new HashSet<>();
    private Long lastTick;
    private Long nextScan;

    PeriodicChecker(ParkingStore store, EligibilityPolicy policy) {
        this.store = store;
        this.policy = policy;
    }

    List<WorkItem> tick(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("tick time must be 0 or greater, got: " + seconds);
        }
        if (lastTick != null && seconds < lastTick) {
            throw new IllegalArgumentException("clock went backwards: tick " + seconds + " is before " + lastTick);
        }
        lastTick = seconds;
        if (nextScan != null && seconds < nextScan) {
            return List.of();   // before the next boundary: no scan, no new work
        }
        nextScan = nextScan == null
                 ? seconds + PERIOD_SECONDS
                 : seconds - (seconds - nextScan) % PERIOD_SECONDS + PERIOD_SECONDS;
        return scan(seconds);
    }

    // Forget what was emitted for this booking, so the next scan may emit it again
    void retry(String bookingId) {
        if (!store.booking().getId().equals(bookingId)) {
            throw new IllegalArgumentException("Unknown booking: " + bookingId);
        }
        emitted.removeIf(key -> key.startsWith(bookingId + "@"));
    }

    private List<WorkItem> scan(long seconds) {
        List<WorkItem> items = new ArrayList<>();
        Booking booking = store.booking();
        Space space = store.space(booking.getSpaceId());
        if (space == null || space.isOpen()) {
            return items;   // only bookings on closed spaces are affected
        }
        String key = booking.getId() + "@" + booking.getVersion() + "/" + policy.getVersion();
        if (emitted.contains(key) || hasActiveProposal(booking)) {
            return items;
        }
        emitted.add(key);
        items.add(new WorkItem(booking.getId(), booking.getVersion(), policy.getVersion(), seconds));
        return items;
    }

    // A PENDING or APPROVED proposal already covers this booking/policy version pair
    private boolean hasActiveProposal(Booking booking) {
        for (Proposal proposal : store.proposals()) {
            boolean active = proposal.getStatus() == ProposalStatus.PENDING
                          || proposal.getStatus() == ProposalStatus.APPROVED;
            if (active && proposal.getBookingId().equals(booking.getId())
                    && proposal.getObservedBookingVersion() == booking.getVersion()
                    && proposal.getObservedPolicyVersion() == policy.getVersion()) {
                return true;
            }
        }
        return false;
    }
}
