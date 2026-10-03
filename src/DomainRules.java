public final class DomainRules {
    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24;
    private DomainRules() {}
    public static void requireDuration(int hours) {
        // TODO D1: reject outside 1..24 with IllegalArgumentException.
        if(hours < MIN_HOURS || hours > MAX_HOURS){
            throw new IllegalArgumentException("duration should be 1..24, got: " + hours);
        }

    }
    public static void requireIdentifier(String id) {
        // TODO D1: non-null [A-Z][A-Z0-9_-]{0,15}.

    

        throw new UnsupportedOperationException("D1 identifier validation");
    }
}
