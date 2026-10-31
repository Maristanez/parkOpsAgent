// A booking that needs attention, found by a periodic scan: it sits on a closed space.
// One work item is emitted per booking version / policy version pair.
public record WorkItem(String bookingId, int bookingVersion, int policyVersion, long detectedAt) { }
