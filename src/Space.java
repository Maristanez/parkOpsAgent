public final class Space{
    private final String id;
    private final boolean open; 
    private final boolean occupied;
    private final boolean accessible;

    public Space(String id, boolean open, boolean occupied, boolean accessible){
        // validate id
        DomainRules.requireIdentifier(id);

        // assign four fields
        this.id = id;
        this.open = open;
        this.occupied = occupied;
        this.accessible = accessible;
    }

    public String getId(){
        return id;
    }
    public boolean isOpen(){
        return open;
    }
    public boolean isOccupied(){
        return occupied;
    }
    public boolean isAccessible(){
        return accessible;
    }

    // Available spot means that it must be open and not occupied 
    public boolean isAvailable(){
        return open && !occupied; 
    }

}