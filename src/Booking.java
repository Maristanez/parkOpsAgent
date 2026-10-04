public final class Booking{
    private final String id;
    private final String spaceId; 
    private final int durationHours;
    private final int version; 
    private final boolean requiresAccessible; 

    public Booking(String id, String spaceId, int durationHours, int version, boolean requiresAccessible){

        // Validate id and spaceId and duration 
        DomainRules.requireIdentifier(id); 
        DomainRules.requireIdentifier(spaceId);

        // must be 1 to 24 
        DomainRules.requireDuration(durationHours); 
        
        // version checks
        if(version < 0){
            throw new IllegalArgumentException("Version must be 0 or greater, got: " + version);
        }

        // setting values
        this.id = id; 
        this.spaceId = spaceId;
        this.durationHours = durationHours;
        this.version = version;
        this.requiresAcessible = requiresAccessible;
    }



    

}