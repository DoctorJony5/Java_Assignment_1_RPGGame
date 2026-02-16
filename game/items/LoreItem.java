// Jonathan Decondé - 3196362
package game.items;

// Poor idea / concept
public class LoreItem {
    private String name;
    private String description;
    private String loreText;
    private int floorFound;
    
    public LoreItem(String name, String description, String loreText, int floorFound) {
        this.name = name;
        this.description = description;
        this.loreText = loreText;
        this.floorFound = floorFound;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLoreText() { return loreText; }
    public int getFloorFound() { return floorFound; }
    
    @Override
    public String toString() {
        return name + " - " + description;
    }
}
