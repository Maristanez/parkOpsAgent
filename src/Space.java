public final class Space{
    private final String id;
    private boolean open; 
    private boolean occupied;
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

    // car takes space, refuses a space if taken
    public void occupy(){
        if(occupied){
            throw new IllegalStateException("Space " + id + " is already occupied");
        }
        occupied = true; 
    }

    // car leave space
    public void release(){
        occupied = false;
    }

    // space stops taking cars
    public void close(){
        open = false; 
    }

}