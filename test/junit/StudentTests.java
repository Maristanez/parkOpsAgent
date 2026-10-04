// Add focused @org.junit.jupiter.api.Test methods here.
// Create fresh objects for each test and assert results AND unchanged state on rejection.
// The supplied wrapper is not a substitute for your own test design.
iimport org.junit.jupiter.api.Test;
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
}
