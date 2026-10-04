// Add focused @org.junit.jupiter.api.Test methods here.
// Create fresh objects for each test and assert results AND unchanged state on rejection.
// The supplied wrapper is not a substitute for your own test design.
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
}
