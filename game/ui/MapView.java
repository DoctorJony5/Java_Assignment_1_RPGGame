package game.ui;

import util.ConsoleColor;

// Map view handles all the map related stuff
public class MapView {
    private final View baseView;
    private boolean useEmojis;
    private int viewportWidth = 100; // Default viewport size
    private int viewportHeight = 28;
    private int playerLevel = 1; // Player level for monster color comparison
    private game.systems.dungeons.DungeonFloor currentFloor; // Reference to dungeon floor for monster info

    public MapView(View baseView, boolean useEmojis) {
        this.baseView = baseView;
        this.useEmojis = useEmojis;
    }

    public void setUseEmojis(boolean useEmojis) {
        this.useEmojis = useEmojis;
    } // settings, change from ASCII to emojis - not well implemented

    public void setViewportSize(int width, int height) {
        this.viewportWidth = Math.max(40, Math.min(120, width));
        this.viewportHeight = Math.max(15, Math.min(60, height));
    }

    // Map rendering system.
    public void showDungeonMap(char[][] map, boolean[][] visible, boolean[][] explored, int playerX, int playerY) {
        showDungeonMap(map, visible, explored, playerX, playerY, false);
    }

    // Set player level for monster coloring
    public void setPlayerLevel(int level) {
        this.playerLevel = level;
    }

    // Set dungeon floor reference for monster info
    public void setDungeonFloor(game.systems.dungeons.DungeonFloor floor) {
        this.currentFloor = floor;
    }

    // Show the map
    // Less complicated than it looks

    // The map is just a massive array of characters
    // But it's also duplicated to know what is visible, and what is explored.
    // we ALSO need to have other info like x and y coordinates and the torch.
    //
    public void showDungeonMap(char[][] map, boolean[][] visible, boolean[][] explored, int playerX, int playerY,
            boolean hasTorch) {
        int fullHeight = map.length;
        int fullWidth = map[0].length;

        // Check if map is smaller than viewport (need padding/centering)
        boolean mapSmallerThanViewport = fullWidth < viewportWidth || fullHeight < viewportHeight;

        int startX, startY, endX, endY, displayWidth, displayHeight;
        int leftPadding = 0, topPadding = 0, bottomPadding = 0;

        if (mapSmallerThanViewport) {
            // Map is smaller than viewport - show entire map centered with padding
            startX = 0;
            startY = 0;
            endX = fullWidth;
            endY = fullHeight;
            displayWidth = fullWidth;
            displayHeight = fullHeight;

            // Calculate padding to center the map
            if (fullWidth < viewportWidth) {
                leftPadding = (viewportWidth - fullWidth) / 2;
                displayWidth = viewportWidth;
            }
            if (fullHeight < viewportHeight) {
                topPadding = (viewportHeight - fullHeight) / 2;
                bottomPadding = viewportHeight - fullHeight - topPadding;
            }
        } else {
            // Normal viewport logic - center on player
            startX = Math.max(0, playerX - viewportWidth / 2);
            startY = Math.max(0, playerY - viewportHeight / 2);
            endX = Math.min(fullWidth, startX + viewportWidth);
            endY = Math.min(fullHeight, startY + viewportHeight);

            // Adjust if viewport goes beyond map edges
            if (endX - startX < viewportWidth && fullWidth >= viewportWidth) {
                startX = Math.max(0, endX - viewportWidth);
            }
            if (endY - startY < viewportHeight && fullHeight >= viewportHeight) {
                startY = Math.max(0, endY - viewportHeight);
            }

            displayWidth = endX - startX;
            displayHeight = endY - startY;
        }

        // Top border
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "┌" + "─".repeat(displayWidth) + "┐" + ConsoleColor.RESET);

        // Top padding (if map is smaller than viewport)
        for (int i = 0; i < topPadding; i++) {
            baseView.printLine(ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET + " ".repeat(displayWidth)
                    + ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        }

        // Map content (viewport only)
        for (int y = startY; y < endY; y++) {
            StringBuilder row = new StringBuilder();
            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);

            // Left padding (if map is smaller than viewport)
            if (leftPadding > 0) {
                row.append(" ".repeat(leftPadding));
            }

            for (int x = startX; x < endX; x++) {
                boolean isPlayer = (x == playerX && y == playerY);

                if (!visible[y][x] && !explored[y][x]) {
                    row.append(" ");
                    continue;
                }

                char tile = map[y][x];

                // Hide traps unless within 2 tile range OR player has torch
                if (tile == '^' && visible[y][x]) {
                    int dx = x - playerX;
                    int dy = y - playerY;
                    int distSq = dx * dx + dy * dy;

                    // Show trap only if within 2 tiles OR has torch
                    if (!hasTorch && distSq > 4) {
                        tile = '.'; // Display as floor if too far and no torch
                    }
                }

                if (!visible[y][x]) {
                    // Dimmed explored tiles
                    row.append(ConsoleColor.BLACK).append(dimChar(tile)).append(ConsoleColor.RESET);
                    continue;
                }

                if (isPlayer) {
                    row.append(ConsoleColor.BRIGHT_YELLOW).append('@').append(ConsoleColor.RESET);
                } else {
                    row.append(colorizeTile(tile, x, y));
                }
            }

            // Right padding (if map is smaller than viewport)
            if (leftPadding > 0) {
                int rightPadding = displayWidth - (endX - startX) - leftPadding;
                if (rightPadding > 0) {
                    row.append(" ".repeat(rightPadding));
                }
            }

            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);
            baseView.printLine(row.toString());
        }

        // Bottom padding (if map is smaller than viewport)
        for (int i = 0; i < bottomPadding; i++) {
            baseView.printLine(ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET + " ".repeat(displayWidth)
                    + ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        }

        // Bottom border
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "└" + "─".repeat(displayWidth) + "┘" + ConsoleColor.RESET);
    }

    // Show ENTIRE dungeon map - iffy
    public void showFullDungeonMap(char[][] map, boolean[][] visible, boolean[][] explored, int playerX,
            int playerY, boolean hasTorch) {
        int fullHeight = map.length;
        int fullWidth = map[0].length;

        // transpose map if too large
        boolean transpose = fullHeight > fullWidth;
        int displayWidth = transpose ? fullHeight : fullWidth;
        int displayHeight = transpose ? fullWidth : fullHeight;

        // Top border
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "┌" + "─".repeat(displayWidth) + "┐" + ConsoleColor.RESET);

        for (int dy = 0; dy < displayHeight; dy++) {
            StringBuilder row = new StringBuilder();
            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);

            for (int dx = 0; dx < displayWidth; dx++) {
                // Map display coordinates (dx,dy) might be transposed relative to map
                int x = transpose ? dy : dx;
                int y = transpose ? dx : dy;

                boolean isPlayer = (x == playerX && y == playerY);

                // If the tile hasn't been explored, show blank (fog of war)
                if (!explored[y][x]) {
                    row.append(' ');
                    continue;
                } // technically everything unexplored is a #

                char tile = map[y][x];

                // Hide traps unless within 2 tile range OR player has torch
                if (tile == '^' && explored[y][x]) {
                    int ddx = x - playerX;
                    int ddy = y - playerY;
                    int distSq = ddx * ddx + ddy * ddy;
                    // a very simple calculation to guess if it's close to the player or not.
                    // it's far from ideal though.
                    if (!hasTorch && distSq > 4) {
                        tile = '.'; // display as floor if too far
                    }
                }

                if (!visible[y][x]) {
                    // Dim explored but not currently visible tiles
                    // This looks weird on some consoles, no idea about Eclipse.
                    row.append(ConsoleColor.BLACK).append(dimChar(tile)).append(ConsoleColor.RESET);
                    continue;
                }

                if (isPlayer) {
                    row.append(ConsoleColor.BRIGHT_YELLOW).append('@').append(ConsoleColor.RESET);
                } else {
                    row.append(colorizeTile(tile, x, y));
                }
            }

            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);
            baseView.printLine(row.toString());
        }

        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "└" + "─".repeat(displayWidth) + "┘" + ConsoleColor.RESET);
    }

    // Map legend
    // I haven't touched this much
    // It's really not up to date sadly
    // We don't really use doors or a merchant or etc
    public void showMapLegend() {
        baseView.printLine("");
        baseView.printLine(
                ConsoleColor.BRIGHT_CYAN + "╔════════════════════════════════════════╗" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BOLD +
                "           MAP LEGEND                  " +
                ConsoleColor.RESET + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(
                ConsoleColor.BRIGHT_CYAN + "╠════════════════════════════════════════╣" + ConsoleColor.RESET);

        String[][] legend = {
                { "@", "BRIGHT_YELLOW", "You (the player)" },
                { "#", "WHITE", "Wall / solid barrier" },
                { ".", "BRIGHT_BLACK", "Floor / walkable space" },
                { "+", "YELLOW", "Closed door" },
                { "/", "BRIGHT_GREEN", "Open door" },
                { ">", "YELLOW", "Stairs down (next floor)" },
                { "<", "YELLOW", "Stairs up (previous floor)" },
                { "^", "BRIGHT_RED", "Trap (danger!)" },
                { "C", "YELLOW", "Chest (treasure)" },
                { "?", "BRIGHT_MAGENTA", "Puzzle or mystery" },
                { "$", "YELLOW", "Shop / merchant" },
                { "&", "BRIGHT_CYAN", "Altar / safe zone" },
                { "X", "RED", "Corpse" },
        };

        for (String[] entry : legend) {
            String symbol = entry[0];
            String colorName = entry[1];
            String description = entry[2];
            String colorCode = getColorCode(colorName);

            baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                    colorCode + symbol + ConsoleColor.RESET +
                    "  - " + description +
                    " ".repeat(Math.max(0, 36 - description.length())) +
                    ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        }

        // print monsters and colours
        // THis was a last minute addition that has VERY MANY faults and errors.
        baseView.printLine(
                ConsoleColor.BRIGHT_CYAN + "╠════════════════════════════════════════╣" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " + ConsoleColor.BOLD +
                "Monsters (color coded by level):     " +
                ConsoleColor.RESET + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.BLUE + "M" + ConsoleColor.RESET + "       - Normal (lower level)" +
                "         " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.MAGENTA + "M" + ConsoleColor.RESET + "       - Normal (same level)" +
                "          " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.RED + "M" + ConsoleColor.RESET + "       - Normal (higher level)" +
                "        " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.BLUE + "!" + ConsoleColor.RESET + "       - Elite (lower level)" +
                "          " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.MAGENTA + "!" + ConsoleColor.RESET + "       - Elite (same level)" +
                "           " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.RED + "!" + ConsoleColor.RESET + "       - Elite (higher level)" +
                "         " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " +
                ConsoleColor.BRIGHT_RED + "B" + ConsoleColor.RESET + "       - Boss (extreme threat)" +
                "        " + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);

        baseView.printLine(
                ConsoleColor.BRIGHT_CYAN + "╚════════════════════════════════════════╝" + ConsoleColor.RESET);
        baseView.printLine("");
    }

    // Header section
    public void showFloorHeader(int floorNumber) {
        baseView.clear();
        String title = "FLOOR " + floorNumber + " - Daedalus's Dungeon";
        int width = 80;

        baseView.printLine(ConsoleColor.BRIGHT_RED + "═".repeat(width) + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                " ".repeat((width - title.length()) / 2) + title + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED + "═".repeat(width) + ConsoleColor.RESET);
    }

    // Helper / util methods
    private char dimChar(char c) {
        if (c == '@')
            return '@';
        if (c == 0)
            return ' ';
        return c;
    }

    private String colorizeTile(char tile, int x, int y) {
        // Emoji mode
        // This is a poor idea
        // It also hurts to look at
        // but hey; simple / cheap feature
        if (useEmojis) {
            return switch (tile) {
                case '#' -> "⬜";
                case '.' -> "◼️";
                case '+' -> "🚪";
                case '/' -> "🟢";
                case '>', '<' -> "🟨";
                case '^' -> "💥";
                case 'X' -> "💀";
                case 'M' -> "👹"; // Normal monster
                case '!' -> "❗"; // Elite monster
                case 'B' -> "🐉"; // Boss
                case '*' -> "⭐";
                case 'C' -> "📦";
                case '?' -> "❓";
                case '$' -> "🏪";
                case '&' -> "⛪";
                case 'E' -> "🚪"; // Exit
                default -> String.valueOf(tile);
            };
        }

        // ASCII color mode - danger-based coloring
        return switch (tile) {
            // Structural - neutral
            case '#' -> ConsoleColor.WHITE + "#" + ConsoleColor.RESET;
            case '.' -> ConsoleColor.BRIGHT_BLACK + "." + ConsoleColor.RESET;

            // Doors - green=safe, yellow=caution
            case '+' -> ConsoleColor.YELLOW + "+" + ConsoleColor.RESET;
            case '/' -> ConsoleColor.BRIGHT_GREEN + "/" + ConsoleColor.RESET;

            // Stairs - YELLOW (safe exits)
            case '>', '<' -> ConsoleColor.YELLOW + tile + ConsoleColor.RESET;

            // Traps - BRIGHT RED
            case '^' -> ConsoleColor.BRIGHT_RED + "^" + ConsoleColor.RESET;
            case 'X' -> ConsoleColor.RED + "X" + ConsoleColor.RESET;

            // Monsters - NEW SYSTEM with level-based coloring
            // M = normal monster (colored by level comparison)
            // ! = elite monster (colored by level comparison)
            // B = boss (always bright red)
            case 'M', '!' -> getMonsterColor(tile, x, y);
            case 'B' -> ConsoleColor.BRIGHT_RED + "B" + ConsoleColor.RESET; // Boss = Bright Red (maximum threat)

            // Items/Interactions - bright colors
            case '*' -> ConsoleColor.BRIGHT_YELLOW + "*" + ConsoleColor.RESET;
            case 'C' -> ConsoleColor.YELLOW + "C" + ConsoleColor.RESET;
            case '?' -> ConsoleColor.BRIGHT_MAGENTA + "?" + ConsoleColor.RESET;
            case '$' -> ConsoleColor.YELLOW + "$" + ConsoleColor.RESET;
            case '&' -> ConsoleColor.BRIGHT_CYAN + "&" + ConsoleColor.RESET;
            case 'E' -> ConsoleColor.BRIGHT_YELLOW + "E" + ConsoleColor.RESET; // Exit

            default -> String.valueOf(tile);
        };
    }

    // Get color for monster based on level comparison
    private String getMonsterColor(char symbol, int x, int y) {
        if (currentFloor == null) {
            // Fallback colors if no floor reference
            return symbol == 'M'
                    ? ConsoleColor.CYAN + "M" + ConsoleColor.RESET
                    : ConsoleColor.BRIGHT_RED + "!" + ConsoleColor.RESET;
        }

        // Get room at this location
        game.systems.dungeons.Room room = currentFloor.getRoomAt(x, y);
        if (room == null || room.getMonsters().isEmpty()) {
            // No monsters, use default coloring
            return symbol == 'M'
                    ? ConsoleColor.CYAN + "M" + ConsoleColor.RESET
                    : ConsoleColor.MAGENTA + "!" + ConsoleColor.RESET;
        }

        // Get first monster in room to determine level
        game.entities.monsters.Monster monster = room.getMonsters().get(0);
        int monsterLevel = monster.getLevel();
        int levelDiff = monsterLevel - playerLevel;

        // Determine color based on level difference
        // Lower level (2+ levels below): BLUE
        // Same-ish level (within 2 levels): PURPLE/MAGENTA
        // Higher level (2+ levels above): RED
        String color;
        if (levelDiff <= -2) {
            // Lower level enemies - blue
            color = ConsoleColor.BLUE;
        } else if (levelDiff >= 2) {
            // Higher level enemies - red
            color = ConsoleColor.RED;
        } else {
            // Same-ish level - purple/magenta
            color = ConsoleColor.MAGENTA;
        }

        return color + symbol + ConsoleColor.RESET;
    }

    private String getColorCode(String colorName) {
        return switch (colorName) {
            case "BRIGHT_YELLOW" -> ConsoleColor.BRIGHT_YELLOW;
            case "WHITE" -> ConsoleColor.WHITE;
            case "BRIGHT_BLACK" -> ConsoleColor.BRIGHT_BLACK;
            case "YELLOW" -> ConsoleColor.YELLOW;
            case "BRIGHT_GREEN" -> ConsoleColor.BRIGHT_GREEN;
            case "BRIGHT_RED" -> ConsoleColor.BRIGHT_RED;
            case "RED" -> ConsoleColor.RED;
            case "BRIGHT_MAGENTA" -> ConsoleColor.BRIGHT_MAGENTA;
            case "BRIGHT_CYAN" -> ConsoleColor.BRIGHT_CYAN;
            case "GREEN" -> ConsoleColor.GREEN;
            case "CYAN" -> ConsoleColor.CYAN;
            case "MAGENTA" -> ConsoleColor.MAGENTA;
            default -> ConsoleColor.RESET;
        };
    }
}
