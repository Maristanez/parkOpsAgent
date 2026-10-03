import java.util.regex.Pattern;

public final class DomainRules {
    // constants 
    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24;
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z][A-Z0-9_-]{0,15}");


    private DomainRules() {}
    public static void requireDuration(int hours) {
        // Completed D1: reject outside 1..24 with IllegalArgumentException.
        if(hours < MIN_HOURS || hours > MAX_HOURS){
            throw new IllegalArgumentException("duration should be 1..24, got: " + hours);
        }

    }
    public static void requireIdentifier(String id) {
        // Completed D1: non-null [A-Z][A-Z0-9_-]{0,15}.

        // check null and if string matches regex pattern
        if(id == null || !ID_PATTERN.matcher(id).matches()){
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }

    }
}
