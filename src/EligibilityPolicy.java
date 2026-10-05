public final class EligibilityPolicy{
    private static final int VERSION = 1;

    public int getVersion(){
        return VERSION;
    }

    // Policy v1: the target must differ from the booking's current space and be available.
    // The booking's accessibility requirement is deliberately unused until policy v2 (D3).
    public boolean isEligible(Booking booking, Space target){
        if(booking == null || target == null){
            throw new IllegalArgumentException("booking and target are required");
        }
        boolean differentSpace = !target.getId().equals(booking.getSpaceId());
        return differentSpace && target.isAvailable();
    }
}
