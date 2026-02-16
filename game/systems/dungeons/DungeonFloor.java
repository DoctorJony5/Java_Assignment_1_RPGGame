// Jonathan Decondé - 3196362

package game.systems.dungeons;

import game.Difficulty;
import game.items.MonsterFactory;
import game.systems.DungeonMasterSystem;
import game.systems.LoreLibrary;
import util.DynArray;
import util.RNG;

// A single "floor" of a "dungeon"
public class DungeonFloor {
    private int floorNumber;
    private Difficulty difficulty;
    private char[][] map;
    private boolean[][] visible;
    private boolean[][] explored;
    private DynArray<Room> rooms;
    private int playerX;
    private int playerY;
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private RNG rng;
    private long seed;
    private DungeonMasterSystem.Tuning tuning;
    private LoreLibrary loreLibrary;
    private int mapWidth;
    private int mapHeight;
    private static final char WALL = '#';
    private static final char FLOOR_TILE = '.';
    private static final char CORRIDOR = '.';
    private static final char DOOR_CLOSED = '+';
    private static final char DOOR_OPEN = '/';
    private static final char STAIRS_DOWN = '>';
    private static final char STAIRS_UP = '<';
    private static final char EXIT = 'E';
    private static final char TRAP = '^';
    private static final int BASE_VISION_RADIUS = 5;
    private static final int EXTENDED_VISION_RADIUS = 7;
    private static final int TORCH_BASE_VISION = 5;
    private int visionRadius = BASE_VISION_RADIUS;
    private int extendedVisionRadius = EXTENDED_VISION_RADIUS;
    private int playerLevel; // Player level for monster scaling

    // Boss system
    private boolean isBossFloor; // Every 5th floor has a special boss
    private Room bossRoom; // Reference to the boss room
    private int bossX; // Current boss X position
    private int bossY; // Current boss Y position
    private int actionCounter; // Track actions for boss roar timing
    private boolean bossRevealed; // Is boss currently visible on map?
    private int bossRevealExpires; // When boss reveal period expires

    // Exit system
    private boolean hasExit; // Does this floor have an exit?
    private Room exitRoom; // Reference to the exit room

    public DungeonFloor(int floorNumber, Difficulty difficulty) {
        this(floorNumber, difficulty, 100, 30, System.nanoTime(), new DungeonMasterSystem.Tuning(), 1);
    }

    // Constructor
    public DungeonFloor(int floorNumber, Difficulty difficulty, int mapWidth, int mapHeight, long seed,
            DungeonMasterSystem.Tuning tuning, int playerLevel) {
        this.floorNumber = floorNumber;
        this.difficulty = difficulty;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.seed = seed;
        this.tuning = tuning != null ? tuning : new DungeonMasterSystem.Tuning();
        this.playerLevel = playerLevel;
        this.map = new char[mapHeight][mapWidth];
        this.visible = new boolean[mapHeight][mapWidth];
        this.explored = new boolean[mapHeight][mapWidth];
        this.rooms = new DynArray<>();
        this.rng = new RNG(seed);
        this.loreLibrary = new LoreLibrary(rng);

        generate();
    }

    // Constructor for backward compatibility
    public DungeonFloor(int floorNumber, Difficulty difficulty, int mapWidth, int mapHeight, long seed,
            DungeonMasterSystem.Tuning tuning) {
        this(floorNumber, difficulty, mapWidth, mapHeight, seed, tuning, 1);
    }

    private void generate() {
        initializeMap();

        this.isBossFloor = (floorNumber > 0 && floorNumber % 5 == 0 && floorNumber != 1);

        generateRooms();
        generateCorridors();
        createChoicePaths();
        markRoomTypes();
        placeLoreItems();
        placeMimics();
        findPlayerStart();
        findFloorExit();
        sprinkleDetails();
        updateVisibility();

        // I am quite proud of this system.
        // First the rooms are generated, then corridors are generated to connect them
        // We then do everything else, ending with the floor exit and some details and
        // finally player visibility in base.
    }

    private void markRoomTypes() {
        // This is mainly for debugging; mark room type
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            int centerX = room.getX() + room.getWidth() / 2;
            int centerY = room.getY() + room.getHeight() / 2;

            char symbol = FLOOR_TILE;
            switch (room.getType()) {
                case ENCOUNTER:
                    // NEW: Use M for normal monsters, ! for elite
                    symbol = room.isElite() ? '!' : 'M';
                    break;
                case TREASURE:
                    symbol = 'C';
                    break;
                case PUZZLE:
                    symbol = '?';
                    break;
                case BOSS:
                    // NEW: Use B for bosses
                    symbol = 'B';
                    break;
                case SHOP:
                    symbol = '$';
                    break; // Ignore
                case SAFE:
                    symbol = '&';
                    break;
                case EXIT:
                    symbol = 'E';
                    break;
                default:
                    symbol = FLOOR_TILE;
            }
            if (room.isElite()) {
                symbol = '!';
            }

            if (centerX >= 0 && centerX < mapWidth && centerY >= 0 && centerY < mapHeight) {
                map[centerY][centerX] = symbol;
            }
        }
    }

    private char pickMonsterSymbol() {
        // NEW: Return a generic monster symbol
        // M = normal monster, ! = elite, B = boss
        // This is determined by the room type and elite status in the map rendering
        return 'M'; // Default normal monster
    }

    private void initializeMap() {
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                map[y][x] = WALL;
            }
        }
    }

    private void generateRooms() {
        // Use DungeonRooms to generate, assign types, and populate rooms
        DungeonRooms roomGenerator = new DungeonRooms(floorNumber, difficulty, mapWidth, mapHeight, rng, tuning);
        // Scale room count based on map size (larger maps = more rooms)
        int baseRooms = 7 + floorNumber + difficulty.level; // more rooms as difficulty rises - though capped later
        int scaledRooms = (int) (baseRooms * (mapWidth * mapHeight) / (80.0 * 24.0));
        scaledRooms = Math.min(scaledRooms, 18 + difficulty.level * 2);
        rooms = roomGenerator.generateAndPopulateRooms(scaledRooms, playerLevel);

        // Carve rooms into the map
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            carvRoom(room.getX(), room.getY(), room.getWidth(), room.getHeight());
            assignPuzzleType(room);
        }
    }

    private void assignPuzzleType(Room room) {
        if (room.getType() == Room.RoomType.PUZZLE) {
            int rand = rng.nextInt(100);
            if (rand < 50) {
                room.setPuzzleType(Room.PuzzleType.TIC_TAC_TOE);
            } else {
                room.setPuzzleType(Room.PuzzleType.SPHINX_RIDDLE);
            }
        }
    } // The puzzle rooms just reuse some existing code I had for mini games

    private void carvRoom(int x, int y, int width, int height) {
        for (int ry = y; ry < y + height; ry++) {
            for (int rx = x; rx < x + width; rx++) {
                if (rx >= 0 && rx < mapWidth && ry >= 0 && ry < mapHeight) {
                    map[ry][rx] = FLOOR_TILE;
                }
            }
        }
    } // I know that carving is misspelled

    private void generateCorridors() {
        for (int i = 1; i < rooms.size(); i++) {
            Room current = rooms.get(i);
            int cx = current.getX() + current.getWidth() / 2;
            int cy = current.getY() + current.getHeight() / 2;

            Room nearest = rooms.get(0);
            int nx = nearest.getX() + nearest.getWidth() / 2;
            int ny = nearest.getY() + nearest.getHeight() / 2;
            int bestDist = Math.abs(cx - nx) + Math.abs(cy - ny);

            for (int j = 1; j < i; j++) {
                Room candidate = rooms.get(j);
                int px = candidate.getX() + candidate.getWidth() / 2;
                int py = candidate.getY() + candidate.getHeight() / 2;
                int dist = Math.abs(cx - px) + Math.abs(cy - py);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = candidate;
                    nx = px;
                    ny = py;
                }
            }

            carveCorridor(cx, cy, nx, ny);
        }

        // Explanation
        // This method connects each room to its nearest room by varing corridors
        // It loops through all rooms to ensure that all rooms are connected by at least
        // 1 path
        // It does not guarantee full connectivity, but generally works well enough
        // it also purposefully generates L-shaped corridors for easier navigation
    }

    private void carveCorridor(int fromX, int fromY, int toX, int toY) {
        // Carve L-shaped corridors connecting two points using
        // "horizontal-then-vertical" pattern
        // This creates a grid-like dungeon feel where corridors form right angles
        // Algorithm:
        // 1. Carve horizontal line from start X to end X (using start Y)
        // 2. Carve vertical line from start Y to end Y (using end X)
        // Example: (5,5) to (10,15) creates a corridor:
        // (5,5) -> (10,5) [horizontal line]
        // (10,5) -> (10,15) [vertical line]
        // This is simpler than diagonal corridors and creates readable dungeon layouts

        // Carve horizontal corridor (left-to-right or right-to-left)
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        for (int x = minX; x <= maxX; x++) {
            if (x >= 0 && x < mapWidth && fromY >= 0 && fromY < mapHeight) {
                map[fromY][x] = CORRIDOR;
            }
        }

        // Carve vertical corridor (top-to-bottom or bottom-to-top)
        // Note: This connects to the end of the horizontal corridor
        int minY = Math.min(fromY, toY);
        int maxY = Math.max(fromY, toY);
        for (int y = minY; y <= maxY; y++) {
            if (y >= 0 && y < mapHeight && toX >= 0 && toX < mapWidth) {
                map[y][toX] = CORRIDOR;
            }
        }
    }

    private void createChoicePaths() {
        // Creates two branching difficulty paths that both lead to the boss room
        // Gives players meaningful strategic choice: tackle the easier path first
        // (treasure reward)
        // or challenge the harder path (elite enemy, more risk/reward)
        // Both paths converge at the boss, so player choice affects difficulty
        // progression
        //
        // Why this matters: Players feel in control of their challenge level and can
        // adjust
        // strategy on-the-fly ("I'm weak, I'll grab treasure first" vs "I'll go
        // straight to elite combat")
        //
        // TODO: Could enhance this with visual indicators showing which path is which
        // difficulty
        // or add secret paths that reveal based on specific actions/items

        // Idea came from a friend who play tested this to give the player more choice
        // in doing what they want
        // It's not great
        if (rooms.size() < 4) {
            return;
        }

        Room bossRoom = rooms.get(rooms.size() - 1);
        int bossCenterX = bossRoom.getX() + bossRoom.getWidth() / 2;
        int bossCenterY = bossRoom.getY() + bossRoom.getHeight() / 2;

        Room easyRoom = rooms.get(Math.max(1, rooms.size() / 3));
        Room hardRoom = rooms.get(Math.max(2, (rooms.size() * 2) / 3));

        // "Easy" path: Reward room with treasure (no elite enemies, just items)
        // Positioned at 1/3 through the dungeon for early access
        if (easyRoom.getType() != Room.RoomType.BOSS) {
            easyRoom.setType(Room.RoomType.TREASURE);
            easyRoom.setElite(false);
            carveCorridor(easyRoom.getX() + easyRoom.getWidth() / 2,
                    easyRoom.getY() + easyRoom.getHeight() / 2,
                    bossCenterX, bossCenterY);
        }

        // "Hard" path: Challenge room with elite enemy (high difficulty combat for
        // extra rewards)
        // Positioned at 2/3 through the dungeon, so players must progress further
        // before encountering it
        // This creates difficulty progression: easy treasure -> regular encounters ->
        // elite challenge -> boss
        if (hardRoom.getType() != Room.RoomType.BOSS) {
            hardRoom.setType(Room.RoomType.ENCOUNTER);
            hardRoom.setElite(true);

            // Ensure the room has elite monsters
            hardRoom.getMonsters().clear();
            int monsterCount = 1 + rng.nextInt(2); // 1-2 elite monsters for challenge
            for (int i = 0; i < monsterCount; i++) {
                // Use MonsterFactory to create elite monsters with 20% difficulty boost
                game.entities.monsters.Monster eliteMonster = MonsterFactory.createEncounterMonster(playerLevel, true,
                        difficulty, rng);
                eliteMonster.applyDifficulty(difficulty.enemyStatMultiplier * 1.2); // 20% boost for elite challenge
                hardRoom.addMonster(eliteMonster);
            }

            carveCorridor(hardRoom.getX() + hardRoom.getWidth() / 2,
                    hardRoom.getY() + hardRoom.getHeight() / 2,
                    bossCenterX, bossCenterY);
        }
    }

    private void findPlayerStart() {
        if (rooms.size() > 0) {
            Room startRoom = rooms.get(0);
            this.startX = startRoom.getX() + 1;
            this.startY = startRoom.getY() + 1;
            this.playerX = startX;
            this.playerY = startY;
            map[startY][startX] = STAIRS_UP;
        }
    }

    private void placeLoreItems() {
        // Lore
        // Ignore
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.getType() == Room.RoomType.TREASURE) {
                if (rng.nextInt(100) < 15) {
                    room.setLoreItem(loreLibrary.getRandomLore(floorNumber));
                }
            }
        }
    }

    private void placeMimics() {
        // Mimics

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.getType() == Room.RoomType.TREASURE) {
                if (rng.nextInt(100) < 2) {
                    room.setMimic(true);
                }
            }
        }
    }

    private void findFloorExit() {
        if (rooms.size() > 0) {
            Room endRoom = rooms.get(rooms.size() - 1);
            this.endX = endRoom.getX() + endRoom.getWidth() - 2;
            this.endY = endRoom.getY() + endRoom.getHeight() - 2;

            // Check if this floor should have an exit
            boolean shouldHaveExit = shouldFloorHaveExit(floorNumber);
            if (shouldHaveExit) {
                this.hasExit = true;
                this.exitRoom = endRoom;
                endRoom.setType(Room.RoomType.EXIT);
                map[endY][endX] = EXIT;
            } else {
                map[endY][endX] = STAIRS_DOWN;
            }
        } // Not super happy with this implementation.
    }

    private boolean shouldFloorHaveExit(int floorNum) {
        // First exit should appear at the difficulty's firstExitFloor
        // Then every 5 floors after that
        int firstExitFloor = difficulty.firstExitFloor;
        if (floorNum < firstExitFloor) {
            return false;
        }
        return (floorNum - firstExitFloor) % 5 == 0;
    }

    private void sprinkleDetails() {
        // Adding traps !
        for (int y = 1; y < mapHeight - 1; y++) {
            for (int x = 1; x < mapWidth - 1; x++) {
                // Only place traps on floor tiles that are inside rooms
                if (map[y][x] == FLOOR_TILE) {
                    // Check if this tile is inside a room
                    boolean inRoom = false;
                    for (int i = 0; i < rooms.size(); i++) {
                        Room room = rooms.get(i);
                        if (room.containsPoint(x, y)) {
                            inRoom = true;
                            break;
                        }
                    }

                    // Only place traps in rooms, not in corridors
                    // Also avoid placing directly before corridor entrances
                    if (inRoom && rng.nextInt(100) < 1) {
                        // Check if adjacent tiles are corridors
                        boolean adjacentToCorridor = false;
                        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
                        for (int[] dir : dirs) {
                            int nx = x + dir[0];
                            int ny = y + dir[1];
                            if (nx >= 0 && nx < mapWidth && ny >= 0 && ny < mapHeight) {
                                char neighborTile = map[ny][nx];
                                boolean neighborInRoom = false;
                                for (int i = 0; i < rooms.size(); i++) {
                                    if (rooms.get(i).containsPoint(nx, ny)) {
                                        neighborInRoom = true;
                                        break;
                                    }
                                }
                                if (!neighborInRoom && (neighborTile == FLOOR_TILE || neighborTile == CORRIDOR)) {
                                    adjacentToCorridor = true;
                                    break;
                                }
                            }
                        }

                        // Only place trap if not adjacent to corridor
                        if (!adjacentToCorridor) {
                            map[y][x] = TRAP;
                        }
                    }
                }
            }
        }
    }

    // Getters
    public int getFloorNumber() {
        return floorNumber;
    }

    public char[][] getMap() {
        return map;
    }

    public int getPlayerX() {
        return playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public boolean hasExitRoom() {
        return hasExit;
    }

    public Room getExitRoom() {
        return exitRoom;
    }

    public DynArray<Room> getRooms() {
        return rooms;
    }

    public boolean[][] getVisibleMask() {
        return visible;
    }

    public boolean[][] getExploredMask() {
        return explored;
    }

    // Exploration Ratio
    // Was to be used for achievements but no
    public double getExploredRatio() {
        int walkable = 0;
        int seen = 0;
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                if (map[y][x] != WALL) {
                    walkable++;
                    if (explored[y][x]) {
                        seen++;
                    }
                }
            }
        }
        if (walkable == 0) {
            return 0.0;
        }
        return (double) seen / (double) walkable;
    }

    // Utility methods for game interactions
    // Was in util but this is smarter. ish
    public char getTileAt(int x, int y) {
        if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
            return map[y][x];
        }
        return WALL; // everything starts out as wall
        // Wall is safe
    }

    public void removeTrap(int x, int y) {
        if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
            if (map[y][x] == TRAP) {
                map[y][x] = FLOOR_TILE;
            }
        }
    }

    public void clearRoomMonsters(Room room) {
        // Remove all monsters using new unified symbol system
        // M = normal monster, ! = elite monster, B = boss
        for (int y = room.getY(); y < room.getY() + room.getHeight(); y++) {
            for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
                if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
                    char tile = map[y][x];
                    if (tile == 'M' || tile == '!' || tile == 'B') {
                        map[y][x] = FLOOR_TILE;
                    }
                }
            }
        }
        // Monsters don't actually exist, they're just flags / symbols on a map
        // This was the simplest thing to do.
    }

    // Setters
    public void setPlayerPosition(int x, int y) {
        if (isWalkable(x, y)) {
            this.playerX = x;
            this.playerY = y;
            updateVisibility();
        }
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return false;
        }
        if (map[y][x] == WALL) {
            return false;
        }
        if (map[y][x] == DOOR_CLOSED) {
            // There are no doors
            map[y][x] = DOOR_OPEN;
            return true;
        }
        return true;
    }

    public Room getRoomAt(int x, int y) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.containsPoint(x, y)) {
                return room;
            }
        }
        return null;
    }

    public boolean hasReachedEnd() {
        return playerX == endX && playerY == endY;
    }

    public boolean isValid() {
        if (rooms.size() < 3) {
            return false;
        }
        // Verify start and end positions are set
        if (startX == 0 && startY == 0 && endX == 0 && endY == 0) {
            return false;
        }
        // Use BFS to verify path exists from start to end
        return hasPathBFS(startX, startY, endX, endY);
    }

    // "BFS" pathfinding to check validity of Dungeon
    // Used to ensure Dungeon is up to "standards"
    private boolean hasPathBFS(int fromX, int fromY, int toX, int toY) {
        // Quick bounds check
        if (!inBounds(fromX, fromY) || !inBounds(toX, toY)) {
            return false;
        }

        boolean[][] visited = new boolean[mapHeight][mapWidth];
        int[][] queue = new int[mapWidth * mapHeight][2];
        int queueStart = 0;
        int queueEnd = 0;

        queue[queueEnd][0] = fromX;
        queue[queueEnd][1] = fromY;
        queueEnd++;
        visited[fromY][fromX] = true;

        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        // BFS loop
        while (queueStart < queueEnd) {
            int x = queue[queueStart][0];
            int y = queue[queueStart][1];
            queueStart++;

            // Check if we reached the end
            if (x == toX && y == toY) {
                return true;
            }

            // Explore neighbors
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];

                // Check if valid and not visited
                if (inBounds(newX, newY) && !visited[newY][newX]) {
                    char tile = map[newY][newX];

                    if (tile != WALL && tile != TRAP) {
                        visited[newY][newX] = true;
                        queue[queueEnd][0] = newX;
                        queue[queueEnd][1] = newY;
                        queueEnd++;
                    }
                }
            }
        }

        // No path found
        return false;
    }

    public double getPathDifficultyScore() {
        // Debugging
        // I had an idea to always generate 5 dungeons and then pick the "best" one for
        // the player
        // This was part of an idea of a more dynamic and player responsive dungeon
        // generation system, or what I called the Dungeon Master System.
        // Given another month, this might have been very cool.
        if (!inBounds(startX, startY) || !inBounds(endX, endY)) {
            return -1.0;
        }

        // BFS with distance tracking
        boolean[][] visited = new boolean[mapHeight][mapWidth];
        int[][] queue = new int[mapWidth * mapHeight][2];
        int queueStart = 0;
        int queueEnd = 0;

        queue[queueEnd][0] = startX;
        queue[queueEnd][1] = startY;
        queueEnd++;
        visited[startY][startX] = true;

        int pathLength = 0;
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (queueStart < queueEnd) {
            int x = queue[queueStart][0];
            int y = queue[queueStart][1];
            queueStart++;
            pathLength++;

            if (x == endX && y == endY) {
                double directDistance = Math
                        .sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
                return pathLength / Math.max(directDistance, 1.0);
            }

            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];

                if (inBounds(newX, newY) && !visited[newY][newX]) {
                    char tile = map[newY][newX];
                    if (tile != WALL && tile != TRAP) {
                        visited[newY][newX] = true;
                        queue[queueEnd][0] = newX;
                        queue[queueEnd][1] = newY;
                        queueEnd++;
                    }
                }
            }
        }

        return -1.0;
    }

    private boolean isOpaque(int x, int y) {
        char t = map[y][x];
        return t == WALL || t == DOOR_CLOSED;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < mapWidth && y >= 0 && y < mapHeight;
    }

    private boolean hasLineOfSight(int x0, int y0, int x1, int y1, int maxRadiusSq) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int distSq = dx * dx + dy * dy;
        if (distSq > maxRadiusSq)
            return false;

        int stepX = Integer.compare(dx, 0);
        int stepY = Integer.compare(dy, 0);
        dx = Math.abs(dx);
        dy = Math.abs(dy);

        int err = dx - dy;
        int cx = x0;
        int cy = y0;

        while (!(cx == x1 && cy == y1)) {
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                cx += stepX;
            }
            if (e2 < dx) {
                err += dx;
                cy += stepY;
            }

            if (!inBounds(cx, cy))
                return false;
            if (cx == x1 && cy == y1) {
                return true;
            }
            if (isOpaque(cx, cy)) {
                return false;
            }
        }
        return true;
    }

    public void updateVisibility() {
        // Recalculate which tiles are visible from player's current position
        // This happens every move to show dynamic "fog of war" effect
        // Uses line-of-sight algorithm with different vision radiuses based on terrain

        // TODO: Consider implementing better solution for better performance
        // Clear visible tiles (but keep explored tiles marked for map memory)
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                visible[y][x] = false;
            }
        }

        // Check line of sight from player to every tile on the map
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                char tile = map[y][x];
                // Vision radiuses vary based on terrain
                // Normal terrain uses base vision radius (can see through corridors)
                int radiusSq = visionRadius * visionRadius;
                // Walls and doors have extended sight lines
                // This simulates being able to see "around" corners slightly
                // Gives tactical information about room layouts without unrealistic omniscience
                if (tile == WALL) {
                    radiusSq = extendedVisionRadius * extendedVisionRadius;
                }
                if (tile == 'B') {
                    radiusSq = extendedVisionRadius * extendedVisionRadius;
                }
                // hasLineOfSight checks both distance (radiusSq) and actual line clarity
                // Returns true if tile is in vision range AND has clear line of sight
                if (hasLineOfSight(playerX, playerY, x, y, radiusSq)) {
                    visible[y][x] = true;
                    explored[y][x] = true; // Mark as explored so it stays on map permanently
                }
            }
        }
        // Player always sees their own tile (even in darkness)
        visible[playerY][playerX] = true;
        explored[playerY][playerX] = true;
    }

    public void revealRandomRooms(int count) {
        for (int i = 0; i < count && rooms.size() > 0; i++) {
            Room room = rooms.get(rng.nextInt(rooms.size()));
            for (int y = room.getY(); y < room.getY() + room.getHeight(); y++) {
                for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
                    explored[y][x] = true;
                    visible[y][x] = true;
                }
            }
        }
    }

    // Vision radius!
    public void setVisionRadius(int radius, int extended) {
        this.visionRadius = Math.max(1, radius);
        this.extendedVisionRadius = Math.max(visionRadius, extended);
        updateVisibility();
    }

    // Update with torch and level
    public void updateVisionWithTorch(boolean hasTorch, int currentFloor) {
        if (hasTorch) {
            int torchVision = TORCH_BASE_VISION + (currentFloor / 5);
            setVisionRadius(torchVision, torchVision + 2);
        } else {
            setVisionRadius(BASE_VISION_RADIUS, EXTENDED_VISION_RADIUS);
        }
    }

    // Should trap be visible systsem
    public boolean isTrapVisible(int trapX, int trapY, boolean hasTorch) {
        int dx = trapX - playerX;
        int dy = trapY - playerY;
        int distSq = dx * dx + dy * dy;

        if (hasTorch) {
            return visible[trapY][trapX];
        } else {
            return distSq <= 4 && visible[trapY][trapX];
        }
    }

    public void moveMonsters() {
        // Find current player room
        Room playerRoom = null;
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.containsPoint(playerX, playerY)) {
                playerRoom = room;
                break;
            }
        }

        // Move monsters in each room
        int monsterVision = 4 + difficulty.level * 2; // higher difficulty extends monster sight
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.getMonsters().size() == 0)
                continue;

            // Move if player is in the same room or within "hearing" distance - bad
            // implementation
            boolean shouldMove = (room == playerRoom);
            if (!shouldMove) {
                shouldMove = rng.nextInt(10) < 2; // 20% chance for idle wandering
            }
            if (!shouldMove)
                continue;

            // Find all monster positions in this room
            for (int y = room.getY(); y < room.getY() + room.getHeight(); y++) {
                for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
                    if (!inBounds(x, y)) {
                        continue;
                    }
                    char tile = map[y][x];
                    // Check if this is a monster symbol
                    // M = normal monster, ! = elite monster, B = boss
                    if (tile == 'M' || tile == '!' || tile == 'B') {

                        int dist = Math.abs(x - playerX) + Math.abs(y - playerY);

                        int moveChance = 30 + (dist <= monsterVision ? 30 : 0);
                        if (rng.nextInt(100) < moveChance) {
                            int dx = 0, dy = 0;
                            if (room == playerRoom && dist <= monsterVision) {
                                // Move towards player
                                if (x < playerX)
                                    dx = 1;
                                else if (x > playerX)
                                    dx = -1;
                                if (y < playerY)
                                    dy = 1;
                                else if (y > playerY)
                                    dy = -1;

                                // Add some randomness (50% chance to move in calculated direction)
                                if (rng.nextInt(2) == 0) {
                                    dx = rng.nextInt(3) - 1; // -1, 0, or 1
                                    dy = rng.nextInt(3) - 1;
                                }
                            } else {
                                // Random wander
                                dx = rng.nextInt(3) - 1; // -1, 0, or 1
                                dy = rng.nextInt(3) - 1;
                            }

                            int newX = x + dx;
                            int newY = y + dy;

                            // Check if new position is valid and in same room
                            if (!inBounds(newX, newY)) {
                                continue;
                            }

                            if (room.containsPoint(newX, newY) &&
                                    map[newY][newX] == FLOOR_TILE) {
                                // Move the monster
                                map[newY][newX] = tile;
                                map[y][x] = FLOOR_TILE;
                            }
                        }
                    }
                }
            }
        }
    }

    // Boss stuff 😔
    public void initializeBossRoom() {
        if (!isBossFloor || rooms.size() == 0) {
            return;
        }

        // Get the BOSS room (usually the last room)
        this.bossRoom = rooms.get(rooms.size() - 1);
        if (bossRoom.getType() != Room.RoomType.BOSS) {
            return;
        }

        // Initialize boss position at room center
        this.bossX = bossRoom.getX() + bossRoom.getWidth() / 2;
        this.bossY = bossRoom.getY() + bossRoom.getHeight() / 2;
        this.actionCounter = 0;
        this.bossRevealed = false;
    }

    public boolean isBossFloor() {
        return isBossFloor;
    }

    public Room getBossRoom() {
        return bossRoom;
    }

    public void incrementActionCounter() {
        if (isBossFloor && bossRoom != null) {
            actionCounter++;
        }
    }

    public int getActionCounter() {
        return actionCounter;
    }

    public void resetActionCounter() {
        actionCounter = 0;
    }

    public int getBossX() {
        return bossX;
    }

    public int getBossY() {
        return bossY;
    }

    public void setBossPosition(int x, int y) {
        if (isBossFloor && bossRoom != null && bossRoom.containsPoint(x, y)) {
            this.bossX = x;
            this.bossY = y;
        }
    }

    public boolean isBossRevealed() {
        return bossRevealed;
    }

    public void setBossRevealed(boolean revealed) {
        this.bossRevealed = revealed;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public int getBossRevealExpires() {
        return bossRevealExpires;
    }

    public void setBossRevealExpires(int expires) {
        this.bossRevealExpires = expires;
    }

    public boolean isValidFloor(int x, int y) {
        if (!inBounds(x, y)) {
            return false;
        }
        char tile = map[y][x];
        // Boss can only move on floor tiles or other valid walkable tiles
        return tile == FLOOR_TILE || tile == CORRIDOR || tile == DOOR_OPEN || tile == STAIRS_DOWN || tile == STAIRS_UP;
    }
}
