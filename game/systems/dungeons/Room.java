// Jonathan Decondé - 3196362
package game.systems.dungeons;

import game.entities.monsters.Monster;
import game.items.LoreItem;
import util.DynArray;

/**
 * Room - A single room in a dungeon floor.
 */
public class Room {
    public enum RoomType {
        EMPTY,
        ENCOUNTER, // Monster(s)
        TREASURE, // Chest with loot
        PUZZLE, // Puzzle challenge
        BOSS, // Boss monster
        SHOP, // NPC trader - not exactly implemented
        SAFE, // Safe room - saves
        EXIT // Exit to leave dungeon
    }

    public enum PuzzleType {
        NONE,
        TIC_TAC_TOE,
        SPHINX_RIDDLE
    }

    private int x, y; // Assigning both in the same line for brevity
    private int width, height;
    private RoomType type;
    private PuzzleType puzzleType;
    private DynArray<Monster> monsters;
    private LoreItem loreItem;
    private boolean isMimic;
    private boolean explored;
    private boolean completed;
    private boolean elite;
    private int relootCount; // Tracks how many times this room has been looted

    public Room(int x, int y, int width, int height, RoomType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.puzzleType = PuzzleType.NONE;
        this.monsters = new DynArray<>();
        this.loreItem = null;
        this.isMimic = false;
        this.explored = false;
        this.completed = false;
        this.relootCount = 0;
    }

    public void addMonster(Monster monster) {
        monsters.add(monster);
    }

    public boolean containsPoint(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RoomType getType() {
        return type;
    }

    public PuzzleType getPuzzleType() {
        return puzzleType;
    }

    public DynArray<Monster> getMonsters() {
        return monsters;
    }

    public boolean isExplored() {
        return explored;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isElite() {
        return elite;
    }

    // Setters
    public void setExplored(boolean explored) {
        this.explored = explored;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setPuzzleType(PuzzleType puzzleType) {
        this.puzzleType = puzzleType;
    }

    public void setLoreItem(LoreItem lore) {
        this.loreItem = lore;
    }

    public void setMimic(boolean mimic) {
        this.isMimic = mimic;
    }

    public void setElite(boolean elite) {
        this.elite = elite;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public LoreItem getLoreItem() {
        return loreItem;
    }

    public boolean isMimic() {
        return isMimic;
    }

    public boolean hasLoreItem() {
        return loreItem != null;
    }

    public int getRelootCount() {
        return relootCount;
    }

    public void incrementRelootCount() {
        this.relootCount++;
    }
}
