// Jonathan Decondé - 3196362
package game.systems.dungeons;

import game.Difficulty;
import game.ElementType;
import game.Rarity;
import game.entities.monsters.Monster;
import game.systems.DungeonMasterSystem;
import util.DynArray;
import util.RNG;

// Dungeon Rooms
// Important
public class DungeonRooms {
    private int floorNumber;
    private Difficulty difficulty;
    private RNG rng;
    private DynArray<Room> rooms;
    private int mapWidth;
    private int mapHeight;
    private DungeonMasterSystem.Tuning tuning;

    public DungeonRooms(int floorNumber, Difficulty difficulty) {
        this(floorNumber, difficulty, 100, 30, new RNG(), new DungeonMasterSystem.Tuning()); // Default dimensions
    }

    public DungeonRooms(int floorNumber, Difficulty difficulty, int mapWidth, int mapHeight) {
        this(floorNumber, difficulty, mapWidth, mapHeight, new RNG(), new DungeonMasterSystem.Tuning());
    }

    public DungeonRooms(int floorNumber, Difficulty difficulty, int mapWidth, int mapHeight, RNG rng,
            DungeonMasterSystem.Tuning tuning) {
        this.floorNumber = floorNumber;
        this.difficulty = difficulty;
        this.rng = rng != null ? rng : new RNG();
        this.rooms = new DynArray<>();
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tuning = tuning != null ? tuning : new DungeonMasterSystem.Tuning();
    }

    // Cool map system
    public DynArray<Room> generateDynamicRooms(int maxRooms) {
        // Tutorial mode: ensure more rooms for gradual difficulty
        if (difficulty == Difficulty.TUTORIAL && maxRooms < 6) {
            maxRooms = 6; // Minimum 6 rooms in tutorial
        }

        // These are arbitrary, not sure how to get good ones
        int minRoomWidth = 5;
        int maxRoomWidth = 8;
        int minRoomHeight = 4;
        int maxRoomHeight = 7;

        for (int i = 0; i < maxRooms; i++) {
            int roomWidth = minRoomWidth + rng.nextInt(maxRoomWidth - minRoomWidth + 1);
            int roomHeight = minRoomHeight + rng.nextInt(maxRoomHeight - minRoomHeight + 1);

            // Validate that the room can fit on the map
            if (mapWidth < roomWidth + 3 || mapHeight < roomHeight + 3) {
                continue;
            }

            int roomX = 1 + rng.nextInt(mapWidth - roomWidth - 2);
            int roomY = 1 + rng.nextInt(mapHeight - roomHeight - 2);

            // Check for collision with existing rooms
            boolean collision = false;
            for (int j = 0; j < rooms.size(); j++) {
                Room existing = rooms.get(j);
                if (roomsOverlap(roomX, roomY, roomWidth, roomHeight,
                        existing.getX(), existing.getY(), existing.getWidth(), existing.getHeight())) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                Room room = new Room(roomX, roomY, roomWidth, roomHeight, Room.RoomType.EMPTY);
                rooms.add(room);
            }
        }

        return rooms;
    }

    // Assign types
    public void assignRoomTypes() {
        if (rooms.size() == 0)
            return;

        // First room is always safe
        Room firstRoom = rooms.get(0);
        firstRoom.setType(Room.RoomType.SAFE);

        if (rooms.size() > 1) {
            Room lastRoom = rooms.get(rooms.size() - 1);
            lastRoom.setType(Room.RoomType.BOSS);
        }

        for (int i = 1; i < rooms.size() - 1; i++) {
            Room room = rooms.get(i);
            Room.RoomType type = determineRoomType(i);
            room.setType(type);
        }
    }

    private Room.RoomType determineRoomType(int index) {
        // Prevent bosses in early rooms (already handled in assignRoomTypes, but keep
        // just in case)
        if (index < 2) {
            int roll = rng.nextInt(100);
            if (roll < 50)
                return Room.RoomType.ENCOUNTER;
            else
                return Room.RoomType.TREASURE;
        }

        // Prevent boss in non-final rooms
        if (index == rooms.size() - 1) {
            return Room.RoomType.BOSS;
        }

        // Standard room type distribution
        int encounterWeight = clampWeight(40 + tuning.encounterBias);
        int treasureWeight = clampWeight(30 + tuning.treasureBias);
        int puzzleWeight = 15;
        int shopWeight = 10 + Math.max(0, tuning.safeBias / 2);
        int safeWeight = clampWeight(5 + tuning.safeBias);

        int total = encounterWeight + treasureWeight + puzzleWeight + shopWeight + safeWeight;
        int roll = rng.nextInt(total);
        if (roll < encounterWeight)
            return Room.RoomType.ENCOUNTER;
        roll -= encounterWeight;
        if (roll < treasureWeight)
            return Room.RoomType.TREASURE;
        roll -= treasureWeight;
        if (roll < puzzleWeight)
            return Room.RoomType.PUZZLE;
        roll -= puzzleWeight;
        if (roll < shopWeight)
            return Room.RoomType.SHOP;
        return Room.RoomType.SAFE;
    }

    private int clampWeight(int value) {
        return Math.max(5, value);
    }

    // Populate rooms
    public void populateRooms(int playerLevel) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            if (i == 0) {
                populateStartingRoom(room);
                continue;
            }

            if (room.getType() == Room.RoomType.ENCOUNTER || room.getType() == Room.RoomType.BOSS) {
                populateEnemyRoom(room, playerLevel);
            }
        }
    }

    private void populateStartingRoom(Room room) {
        if (room.getMonsters().size() > 0) {
            room.getMonsters().clear();
        }

        if (rng.nextInt(100) < 75) {
            room.setType(Room.RoomType.TREASURE);
        } else {
            room.setType(Room.RoomType.SAFE);
        }

        room.setCompleted(false);
    }

    private void populateEnemyRoom(Room room, int playerLevel) {
        int eliteChance = 20 + (tuning.encounterBias / 2);
        eliteChance = Math.max(5, Math.min(40, eliteChance));
        boolean elite = room.getType() == Room.RoomType.ENCOUNTER && rng.nextInt(100) < eliteChance;
        room.setElite(elite);

        int monsterCount = room.getType() == Room.RoomType.BOSS ? 1 : (1 + rng.nextInt(3));
        for (int j = 0; j < monsterCount; j++) {
            Monster monster = room.getType() == Room.RoomType.BOSS
                    ? generateBossMonster(playerLevel)
                    : generateMonster(elite, playerLevel);
            monster.applyDifficulty(difficulty.enemyStatMultiplier);
            room.addMonster(monster);
        }
    }

    private Monster generateMonster(boolean elite, int playerLevel) {
        String[] names = { "Goblin", "Orc", "Troll", "Skeleton", "Zombie", "Drake", "Wraith" };
        String name = names.length == 0 ? "Monster" : names[rng.nextInt(names.length)];
        ElementType[] elements = ElementType.values();
        ElementType type = elements.length == 0 ? ElementType.NEUTRAL : elements[rng.nextInt(elements.length)];
        ElementType weakness = elements.length == 0 ? ElementType.NEUTRAL : elements[rng.nextInt(elements.length)];

        Rarity rarity;
        boolean allowElite = true;

        if (difficulty == Difficulty.TUTORIAL) {
            rarity = Rarity.COMMON;
            allowElite = false; // No elite monsters on tutorial
        } else if (difficulty == Difficulty.EASY) {
            if (elite && floorNumber <= 2) {
                rarity = Rarity.COMMON; // Downgrade early elite to common
            } else if (elite) {
                rarity = Rarity.UNCOMMON; // Elite uncommon on later floors
            } else {
                rarity = Rarity.COMMON;
            }
        } else if (difficulty == Difficulty.NORMAL) {
            rarity = elite ? Rarity.RARE : Rarity.COMMON;
        } else {
            // HARD, NIGHTMARE, HELL
            rarity = elite ? Rarity.RARE : Rarity.COMMON;
        }

        // Don't spawn elite if difficulty restricts it
        if (!allowElite) {
            elite = false;
        }

        // Cap monster level based on difficulty and player level
        int maxLevel = getMaxMonsterLevel(playerLevel);
        int level = Math.min(playerLevel + floorNumber + rng.nextInt(3) + (elite ? 2 : 0), maxLevel);

        return new Monster(elite ? "Elite " + name : name, type, weakness, rarity, level);
    }

    private Monster generateBossMonster(int playerLevel) {
        String[] names = { "Minotaur", "Lich", "Hydra", "Warlock" };
        String name = names.length == 0 ? "Boss" : names[rng.nextInt(names.length)];
        ElementType type = ElementType.DARK;
        ElementType weakness = ElementType.LIGHT;

        // Scale boss rarity and level based on difficulty
        Rarity rarity;
        int levelBonus;

        switch (difficulty) {
            case TUTORIAL:
                rarity = Rarity.UNCOMMON;
                levelBonus = 0;
                break;
            case EASY:
                rarity = Rarity.RARE;
                levelBonus = 1;
                break;
            case NORMAL:
                rarity = Rarity.RARE;
                levelBonus = 2;
                break;
            case HARD:
                rarity = Rarity.LEGENDARY;
                levelBonus = 3;
                break;
            case NIGHTMARE:
                rarity = Rarity.MYTHICAL;
                levelBonus = 4;
                break;
            case HELL:
                rarity = Rarity.MYTHICAL;
                levelBonus = 5;
                break;
            default:
                rarity = Rarity.RARE;
                levelBonus = 2;
        }

        int level = Math.min(playerLevel + floorNumber + levelBonus, getMaxMonsterLevel(playerLevel));
        Monster boss = new Monster(name + " (Boss)", type, weakness, rarity, level);
        boss.applyDifficulty(difficulty.enemyStatMultiplier * 1.1);
        return boss;
    }

    private int getMaxMonsterLevel(int playerLevel) {
        // scaling
        int baseLevelCap = playerLevel + floorNumber;

        return switch (difficulty) {
            case TUTORIAL -> Math.min(playerLevel, 2);
            case EASY -> playerLevel + 1;
            case NORMAL -> baseLevelCap + 5;
            case HARD -> baseLevelCap + 20;
            case NIGHTMARE -> baseLevelCap + 40;
            case HELL -> baseLevelCap + 80;
        };
    }

    // Check if rooms overlap
    // This is why I don't have circular rooms
    private boolean roomsOverlap(int x1, int y1, int w1, int h1,
            int x2, int y2, int w2, int h2) {
        return !(x1 + w1 + 1 < x2 || x2 + w2 + 1 < x1 ||
                y1 + h1 + 1 < y2 || y2 + h2 + 1 < y1);
    }

    public DynArray<Room> getRooms() {
        return rooms;
    }

    public void clearRooms() {
        rooms = new DynArray<>();
    }

    public DynArray<Room> generateAndPopulateRooms(int maxRooms, int playerLevel) {
        generateDynamicRooms(maxRooms);
        assignRoomTypes();
        populateRooms(playerLevel);
        return getRooms();
    }

    public DynArray<Room> generateAndPopulateRooms(int maxRooms) {
        return generateAndPopulateRooms(maxRooms, 1); // Default to level 1
    }
}
