public final class Proposal{
    private final String proposalId;
    private final String bookingId;
    private final String targetId;
    private final int observedBookingVersion;
    private final int observedPolicyVersion;
    // Not final: D2 approval and execution change it. No setter, so only this class can.
    private ProposalStatus status;

    public Proposal(String proposalId, String bookingId, String targetId, int observedBookingVersion, int observedPolicyVersion){
        // validate ids
        DomainRules.requireIdentifier(proposalId);
        DomainRules.requireIdentifier(bookingId);
        DomainRules.requireIdentifier(targetId);

        // validate versions
        if(observedBookingVersion < 0){
            throw new IllegalArgumentException("Booking version must be 0 or greater, got: " + observedBookingVersion);
        }
        if(observedPolicyVersion < 1){
            throw new IllegalArgumentException("Policy version must be 1 or greater, got: " + observedPolicyVersion);
        }

        // assign fields
        this.proposalId = proposalId;
        this.bookingId = bookingId;
        this.targetId = targetId;
        this.observedBookingVersion = observedBookingVersion;
        this.observedPolicyVersion = observedPolicyVersion;

        // every proposal starts pending; callers cannot choose its status
        this.status = ProposalStatus.PENDING;
    }

    public String getProposalId(){
        return proposalId;
    }
    public String getBookingId(){
        return bookingId;
    }
    public String getTargetId(){
        return targetId;
    }
    public int getObservedBookingVersion(){
        return observedBookingVersion;
    }
    public int getObservedPolicyVersion(){
        return observedPolicyVersion;
    }
    public ProposalStatus getStatus(){
        return status;
    }

    // Converts to the starter's ProposalView, which stores the status as a String
    public ProposalView toView(){
        return new ProposalView(proposalId, bookingId, targetId,
                                observedBookingVersion, observedPolicyVersion, status.name());
    }
}
