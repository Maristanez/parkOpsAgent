// Add focused @org.junit.jupiter.api.Test methods here.
// Create fresh objects for each test and assert results AND unchanged state on rejection.
// The supplied wrapper is not a substitute for your own test design.
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StudentTests {

    // D1-A: valid durations are kept
    @Test
    void bookingKeepsDurationOne() {
        Booking b = new Booking("B1", "A17", 1, 0, true);
        assertEquals(1, b.getDurationHours());
    }

    @Test
    void bookingKeepsDurationTwentyFour() {
        Booking b = new Booking("B1", "A17", 24, 0, true);
        assertEquals(24, b.getDurationHours());
    }

    // D1-B: invalid durations are rejected
    @Test
    void bookingRejectsDurationZero() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 0, 0, true));
    }

    @Test
    void bookingRejectsDurationTwentyFive() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 25, 0, true));
    }

    // Invalid identifiers are rejected
    @Test
    void bookingRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking(null, "A17", 3, 0, true));
    }

    @Test
    void bookingRejectsLowercaseId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "A17", 3, 0, true));
    }

    @Test
    void bookingRejectsNullSpaceId() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", null, 3, 0, true));
    }

    // Version cannot be negative
    @Test
    void bookingRejectsNegativeVersion() {
        assertThrows(IllegalArgumentException.class, () -> new Booking("B1", "A17", 3, -1, true));
    }

    // Getters return what the constructor received
    @Test
    void bookingKeepsAllFields() {
        Booking b = new Booking("B1", "A17", 3, 0, true);
        assertEquals("B1", b.getId());
        assertEquals("A17", b.getSpaceId());
        assertEquals(3, b.getDurationHours());
        assertEquals(0, b.getVersion());
        assertTrue(b.requiresAccessible());
    }

    // Space availability: open and not occupied
    @Test
    void spaceB12IsAvailable() {
        Space s = new Space("B12", true, false, false);
        assertTrue(s.isAvailable());
    }

    @Test
    void spaceD09IsNotAvailableBecauseOccupied() {
        Space s = new Space("D09", true, true, true);
        assertFalse(s.isAvailable());
    }

    @Test
    void spaceA17IsNotAvailableBecauseClosedAndOccupied() {
        Space s = new Space("A17", false, true, false);
        assertFalse(s.isAvailable());
    }

    @Test
    void spaceClosedButEmptyIsNotAvailable() {
        Space s = new Space("X1", false, false, false);
        assertFalse(s.isAvailable());
    }

    // Invalid space identifiers are rejected
    @Test
    void spaceRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Space(null, true, false, false));
    }

    @Test
    void spaceRejectsLowercaseId() {
        assertThrows(IllegalArgumentException.class, () -> new Space("b12", true, false, false));
    }

    // Getters return what the constructor received
    @Test
    void spaceKeepsAllFields() {
        Space s = new Space("C03", true, false, true);
        assertEquals("C03", s.getId());
        assertTrue(s.isOpen());
        assertFalse(s.isOccupied());
        assertTrue(s.isAccessible());
    }

    // A new proposal is always pending
    @Test
    void newProposalIsPending() {
        Proposal p = new Proposal("P1", "B1", "B12", 0, 1);
        assertEquals(ProposalStatus.PENDING, p.getStatus());
    }

    // Getters return what the constructor received
    @Test
    void proposalKeepsAllFields() {
        Proposal p = new Proposal("P1", "B1", "B12", 0, 1);
        assertEquals("P1", p.getProposalId());
        assertEquals("B1", p.getBookingId());
        assertEquals("B12", p.getTargetId());
        assertEquals(0, p.getObservedBookingVersion());
        assertEquals(1, p.getObservedPolicyVersion());
    }

    // Invalid proposal input is rejected
    @Test
    void proposalRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal(null, "B1", "B12", 0, 1));
    }

    @Test
    void proposalRejectsLowercaseTarget() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "b12", 0, 1));
    }

    @Test
    void proposalRejectsNegativeBookingVersion() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "B12", -1, 1));
    }

    @Test
    void proposalRejectsPolicyVersionZero() {
        assertThrows(IllegalArgumentException.class, () -> new Proposal("P1", "B1", "B12", 0, 0));
    }

    // The view carries the same values, with the status as a String
    @Test
    void proposalViewMatchesProposal() {
        ProposalView v = new Proposal("P1", "B1", "B12", 0, 1).toView();
        assertEquals("P1", v.proposalId());
        assertEquals("B1", v.bookingId());
        assertEquals("B12", v.targetId());
        assertEquals(0, v.bookingVersion());
        assertEquals(1, v.policyVersion());
        assertEquals("PENDING", v.status());
    }

    // Policy v1: the target must differ from the booking's current space and be available
    private static Booking b1() {
        return new Booking("B1", "A17", 3, 0, true);
    }

    @Test
    void policyAcceptsB12EvenThoughB1NeedsAccessible() {
        assertTrue(new EligibilityPolicy().isEligible(b1(), new Space("B12", true, false, false)));
    }

    @Test
    void policyAcceptsC03() {
        assertTrue(new EligibilityPolicy().isEligible(b1(), new Space("C03", true, false, true)));
    }

    @Test
    void policyRejectsOccupiedD09() {
        assertFalse(new EligibilityPolicy().isEligible(b1(), new Space("D09", true, true, true)));
    }

    @Test
    void policyRejectsCurrentSpaceA17() {
        assertFalse(new EligibilityPolicy().isEligible(b1(), new Space("A17", false, true, false)));
    }

    @Test
    void policyRejectsCurrentSpaceEvenWhenAvailable() {
        Booking atB12 = new Booking("B1", "B12", 3, 0, true);
        assertFalse(new EligibilityPolicy().isEligible(atB12, new Space("B12", true, false, false)));
    }

    @Test
    void policyVersionIsOne() {
        assertEquals(1, new EligibilityPolicy().getVersion());
    }

    // StudentApplication starts from the supplied fixture
    @Test
    void applicationStartsFromFixture() {
        StudentApplication app = new StudentApplication();
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(true, app.spaceSnapshot("D09").get("occupied"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertTrue(app.proposals().isEmpty());
    }

    // D1-C: B12 gives a pending proposal; the booking does not move
    @Test
    void proposeB12CreatesPendingProposalAndBookingStaysAtA17() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        ProposalView p = app.propose("B12");

        assertEquals("PENDING", p.status());
        assertEquals("B1", p.bookingId());
        assertEquals("B12", p.targetId());
        assertEquals(0, p.bookingVersion());
        assertEquals(1, p.policyVersion());
        assertTrue(p.proposalId().matches("[A-Z][A-Z0-9_-]{0,15}"));
        assertEquals(List.of(p), app.proposals());
        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(false, app.spaceSnapshot("B12").get("occupied"));
        assertEquals(true, app.spaceSnapshot("A17").get("occupied"));
    }

    // D1-D: occupied D09 is rejected; booking, occupancy and proposals unchanged
    @Test
    void proposeD09IsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();
        Map<String, Object> d09Before = app.spaceSnapshot("D09");

        assertThrows(IllegalArgumentException.class, () -> app.propose("D09"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertEquals(d09Before, app.spaceSnapshot("D09"));
        assertEquals(true, app.spaceSnapshot("D09").get("occupied"));
        assertTrue(app.proposals().isEmpty());
    }

    // D1-E: an unknown target is a controlled error; nothing is created or changed
    @Test
    void proposeUnknownTargetIsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose("Z99"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
        assertEquals(0, app.bookingSnapshot().get("version"));
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void proposeCurrentSpaceA17IsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose("A17"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void proposeMalformedTargetIsRejectedAndNothingChanges() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> bookingBefore = app.bookingSnapshot();

        assertThrows(IllegalArgumentException.class, () -> app.propose(null));
        assertThrows(IllegalArgumentException.class, () -> app.propose("b12"));

        assertEquals(bookingBefore, app.bookingSnapshot());
        assertTrue(app.proposals().isEmpty());
    }

    @Test
    void eachProposalGetsItsOwnApplicationGeneratedId() {
        StudentApplication app = new StudentApplication();
        ProposalView first = app.propose("B12");
        ProposalView second = app.propose("C03");
        assertNotEquals(first.proposalId(), second.proposalId());
        assertEquals(List.of(first, second), app.proposals());
    }

    // D1-F: snapshots and lists handed out cannot change protected state
    @Test
    void bookingSnapshotCannotChangeTheBooking() {
        StudentApplication app = new StudentApplication();
        Map<String, Object> snapshot = app.bookingSnapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("spaceId", "D09"));
        assertEquals("A17", app.bookingSnapshot().get("spaceId"));
    }

    @Test
    void proposalListCannotBeModifiedByCallers() {
        StudentApplication app = new StudentApplication();
        app.propose("B12");
        List<ProposalView> list = app.proposals();
        assertThrows(UnsupportedOperationException.class, () -> list.clear());
        assertEquals(1, app.proposals().size());
    }

    @Test
    void spaceSnapshotRejectsUnknownSpace() {
        StudentApplication app = new StudentApplication();
        assertThrows(IllegalArgumentException.class, () -> app.spaceSnapshot("Z99"));
    }

    // Separate StudentApplication objects do not share state
    @Test
    void applicationsAreIndependent() {
        StudentApplication first = new StudentApplication();
        StudentApplication second = new StudentApplication();
        first.propose("B12");
        assertEquals(1, first.proposals().size());
        assertTrue(second.proposals().isEmpty());
    }
}
