// One successful execution: which proposal moved which booking, from where to where
public record CommitRecord(String proposalId, String bookingId, String fromSpaceId, String toSpaceId,
                           int newBookingVersion) { }
