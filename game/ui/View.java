package game.ui;

import game.GameSettings;
import game.entities.Companion;
import game.entities.Player;
import game.entities.monsters.Monster;
import util.ConsoleColor;

// View manages majority of UI stuff
public class View {
    private boolean useColors;
    private boolean useEmojis;
    private GameSettings settings;
    private static final int CONSOLE_WIDTH = 80;

    // Specialized view delegates
    private MenuView menuView;
    private CombatView combatView;
    private MapView mapView;

    public View(boolean useColors, boolean useEmojis) {
        this.useColors = useColors;
        this.useEmojis = useEmojis;
    }

    // Settings and stuff
    public void initializeViews(GameSettings settings) {
        this.settings = settings;
        this.menuView = new MenuView(this, settings);
        this.combatView = new CombatView(this);
        this.mapView = new MapView(this, useEmojis);
    }

    public MenuView getMenuView() {
        return menuView;
    }

    public CombatView getCombatView() {
        return combatView;
    }

    public MapView getMapView() {
        return mapView;
    }

    public void clear() {
        // ANSI clear screen
        // Doesn't work!
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    public void printLine(String text) {
        System.out.println(text);
    }

    public void printColoredLine(String text, String color) {
        if (useColors) {
            System.out.println(color + text + ConsoleColor.RESET);
        } else {
            System.out.println(text);
        }
    }

    public void print(String text) {
        System.out.print(text);
    }

    public void printCentered(String text) {
        int padding = (CONSOLE_WIDTH - text.length()) / 2;
        printLine(" ".repeat(Math.max(0, padding)) + text);
    }

    // Menus
    public void showMainMenu() {
        clear();
        printCentered(
                ConsoleColor.BRIGHT_YELLOW + ConsoleColor.BOLD + "DAEDALUS'S INFINITE DUNGEON" + ConsoleColor.RESET);
        printLine("");
        printLine("  [1] New Game");
        printLine("  [2] Continue Game");
        printLine("  [3] Scoreboard");
        printLine("  [4] Settings");
        printLine("  [5] Exit");
        printLine("");
        print("Choice > ");
    }

    public void showDifficultyMenu() {
        clear();
        printCentered(ConsoleColor.BRIGHT_CYAN + "SELECT DIFFICULTY" + ConsoleColor.RESET);
        printLine("");
        printLine("  [1] Tutorial - Very Easy (Practice mode)");
        printLine("  [2] Easy - Beginner Friendly");
        printLine("  [3] Normal - Standard (Recommended)");
        printLine("  [4] Hard - Challenging");
        printLine("  [5] Nightmare - Extreme");
        printLine("  [6] Hell - Masochist mode");
        printLine("");
        print("Choice > ");
    }

    public void showSettingsMenu() {
        clear();
        printCentered(ConsoleColor.BRIGHT_GREEN + "SETTINGS" + ConsoleColor.RESET);
        printLine("");
        printLine("  [1] Gameplay Options");
        printLine("  [2] Visual Options");
        printLine("  [3] Debug Mode");
        printLine("  [4] Back to Menu");
        printLine("");
        print("Choice > ");
    }

    public void showFloorHeader(int floor) {
        clear();
        printLine("═══════════════════════════════════════════════════════════════════════════════════");
        printColoredLine("  FLOOR " + floor + " - Daedalus's Dungeon", ConsoleColor.BRIGHT_RED);
        printLine("═══════════════════════════════════════════════════════════════════════════════════");
    }

    public void showPlayerHUD(Player player) {
        // Use viewport width or fallback to default
        int hudWidth = (settings != null && settings.viewportWidth > 0)
                ? Math.max(80, settings.viewportWidth)
                : 80;

        String topBorder = "┌─ ADVENTURER " + "─".repeat(Math.max(0, hudWidth - 14)) + "┐";
        printLine(ConsoleColor.BRIGHT_CYAN + topBorder + ConsoleColor.RESET);

        String nameInfo = String.format("%-40s Level: %-3d │ Floor: %-4d",
                player.getName(), player.getLevel(), player.getFloorReached());
        int namePadding = Math.max(0, hudWidth - nameInfo.length() - 4);
        printLine("│ " + nameInfo + " ".repeat(namePadding) + "  │");

        // HP Bar
        String hpBar = formatBar("HP", player.getHealth(), player.getMaxHealth(), 30, ConsoleColor.BRIGHT_GREEN);
        int hpPadding = Math.max(0, hudWidth - stripAnsiCodes(hpBar).length() - 4);
        printLine("│ " + hpBar + " ".repeat(hpPadding) + "│");

        // Stamina Bar
        String stBar = formatBar("ST", player.getStamina(), player.getMaxStamina(), 30, ConsoleColor.BRIGHT_BLUE);
        int stPadding = Math.max(0, hudWidth - stripAnsiCodes(stBar).length() - 4);
        printLine("│ " + stBar + " ".repeat(stPadding) + "│");

        // This hurts to look at.

        String expInfo = String.format("Experience: %-6d │ Score: %-10d",
                player.getExperience(), player.getTotalScore());
        int expPadding = Math.max(0, hudWidth - expInfo.length() - 4);
        printLine("│ " + expInfo + " ".repeat(expPadding) + "  │");

        if (player.getCompanions().size() > 0) {
            String partyHeader = "Party Morale (Class | Loyalty | Cohesion):";
            int partyHeaderPad = Math.max(0, hudWidth - partyHeader.length() - 4);
            printLine("│ " + partyHeader + " ".repeat(partyHeaderPad) + "  │");

            for (int i = 0; i < player.getCompanions().size(); i++) {
                Companion companion = player.getCompanions().get(i);
                String line = String.format(" - %-10s %-8s L:%-3d C:%-3d %s",
                        companion.getName(),
                        companion.getRole().name(),
                        companion.getLoyalty(),
                        companion.getCohesion(),
                        companion.isEngaged() ? "[Ready]" : "[Disengaged]");
                int linePad = Math.max(0, hudWidth - line.length() - 4);
                printLine("│ " + line + " ".repeat(linePad) + "  │");
            }
        }

        String bottomBorder = "└" + "─".repeat(hudWidth) + "┘";
        printLine(ConsoleColor.BRIGHT_CYAN + bottomBorder + ConsoleColor.RESET);
    }

    public void showCombatHUD(Player player, Monster monster) {
        String leftName = ConsoleColor.BRIGHT_YELLOW + player.getName() + ConsoleColor.RESET;
        String rightName = ConsoleColor.BRIGHT_RED + monster.getName() + ConsoleColor.RESET;
        printLine(padTwoColumns(leftName, rightName, 50));

        String leftHp = formatBar("HP", player.getHealth(), player.getMaxHealth(), 24, ConsoleColor.BRIGHT_GREEN);
        String rightHp = formatBar("HP", monster.getHealth(), monster.getMaxHealth(), 24, ConsoleColor.BRIGHT_RED);
        printLine(padTwoColumns(leftHp, rightHp, 50));

        String leftSt = formatBar("ST", player.getStamina(), player.getMaxStamina(), 24, ConsoleColor.BRIGHT_BLUE);
        String rightSt = formatBar("ST", monster.getStamina(), monster.getMaxStamina(), 24,
                ConsoleColor.BRIGHT_MAGENTA);
        printLine(padTwoColumns(leftSt, rightSt, 50));
    }

    public void printHealthBar(int current, int max, int width) {
        int filled = (int) ((double) current / max * width);
        int empty = width - filled;

        StringBuilder bar = new StringBuilder("HP  [");
        for (int i = 0; i < filled; i++) {
            bar.append(ConsoleColor.BRIGHT_GREEN).append("█").append(ConsoleColor.RESET);
        }
        for (int i = 0; i < empty; i++) {
            bar.append(ConsoleColor.BLACK).append("░").append(ConsoleColor.RESET);
        }
        bar.append("] ").append(current).append("/").append(max);
        printLine(bar.toString());
    }

    public void printStaminaBar(int current, int max, int width) {
        int filled = (int) ((double) current / max * width);
        int empty = width - filled;

        StringBuilder bar = new StringBuilder("ST  [");
        for (int i = 0; i < filled; i++) {
            bar.append(ConsoleColor.BRIGHT_BLUE).append("█").append(ConsoleColor.RESET);
        }
        for (int i = 0; i < empty; i++) {
            bar.append(ConsoleColor.BLACK).append("░").append(ConsoleColor.RESET);
        }
        bar.append("] ").append(current).append("/").append(max);
        printLine(bar.toString());
    }

    private String formatBar(String label, int current, int max, int width, String color) {
        int filled = Math.max(0, Math.min(width, (int) ((double) current / Math.max(1, max) * width)));
        int empty = width - filled;
        StringBuilder bar = new StringBuilder();
        bar.append(String.format("%-3s", label)).append("[");
        for (int i = 0; i < filled; i++) {
            bar.append(color).append("█").append(ConsoleColor.RESET);
        }
        for (int i = 0; i < empty; i++) {
            bar.append(ConsoleColor.BLACK).append("░").append(ConsoleColor.RESET);
        }
        bar.append("] ").append(current).append("/").append(max);
        return bar.toString();
    }

    private String padTwoColumns(String left, String right, int leftWidth) {
        // Count visible characters (strip ANSI codes for padding calculation)
        int leftVisible = stripAnsiCodes(left).length();
        int paddingNeeded = Math.max(0, leftWidth - leftVisible);
        return left + " ".repeat(paddingNeeded) + right;
    }

    private String stripAnsiCodes(String text) {
        // Remove ANSI escape sequences for length calculation
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
        // This isn't perfect, nor exactly necessary.
        // Could be removed
    }

    // Rendering
    public void showDungeonMap(char[][] map, boolean[][] visible, boolean[][] explored, int playerX, int playerY) {
        mapView.showDungeonMap(map, visible, explored, playerX, playerY, false);
    }

    public void showDungeonMap(char[][] map, boolean[][] visible, boolean[][] explored, int playerX, int playerY,
            boolean hasTorch) {
        mapView.showDungeonMap(map, visible, explored, playerX, playerY, hasTorch);
    }

    public void showMapLegend() {
        mapView.showMapLegend();
    }

    public void showMessage(String message) {
        printColoredLine(message, ConsoleColor.WHITE);
    }

    public void showWarning(String message) {
        printColoredLine("⚠ " + message, ConsoleColor.BRIGHT_YELLOW);
    }

    public void showError(String message) {
        printColoredLine("✗ " + message, ConsoleColor.BRIGHT_RED);
    }

    public void showSuccess(String message) {
        printColoredLine("✓ " + message, ConsoleColor.BRIGHT_GREEN);
    }

    public void showInfo(String message) {
        printColoredLine("i " + message, ConsoleColor.BRIGHT_CYAN);
    }

    public void showAchievement(String title, String description) {
        printLine("");
        printColoredLine("╔═══════════════════════════════════════════════════════════╗", ConsoleColor.BRIGHT_YELLOW);
        printColoredLine("║     ACHIEVEMENT UNLOCKED!                                 ║", ConsoleColor.BRIGHT_YELLOW);
        printColoredLine("╠═══════════════════════════════════════════════════════════╣", ConsoleColor.BRIGHT_YELLOW);
        String titleLine = String.format("║  %-56s ║", title);
        String descLine = String.format("║  %-56s ║", description);
        printColoredLine(titleLine, ConsoleColor.BRIGHT_WHITE);
        printColoredLine(descLine, ConsoleColor.BRIGHT_CYAN);
        printColoredLine("╚═══════════════════════════════════════════════════════════╝", ConsoleColor.BRIGHT_YELLOW);
        printLine("");
    }

    // Utilities
    public void pressAnyKey() {
        print("\nPress ENTER to continue...");
        try {
            // Consume the entire line to avoid leaving stray input that can
            // interfere with Scanner-based reads elsewhere
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            reader.readLine();
        } catch (Exception e) {
            // Ignore
        }
    }

    // Optional overload for Scanner-based flows
    public void pressAnyKey(java.util.Scanner scanner) {
        print("\nPress ENTER to continue...");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // Ignore
        }
    }

    public void setUseColors(boolean useColors) {
        this.useColors = useColors;
    }

    public void setUseEmojis(boolean useEmojis) {
        this.useEmojis = useEmojis;
    }

    public String readLine() {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            return reader.readLine();
        } catch (java.io.IOException e) {
            return "";
        }
    }

    // Levelling menu
    public boolean showStatAllocation(Player player) {
        if (player.getUnspentStatPoints() <= 0) {
            return false;
        }

        while (player.getUnspentStatPoints() > 0) {
            printLine("");
            printLine("");
            printColoredLine("╔═════════════════════════════════════════════════════════════╗",
                    ConsoleColor.BRIGHT_YELLOW);
            printColoredLine("║           !   LEVEL UP! ALLOCATE STAT POINTS                ║",
                    ConsoleColor.BRIGHT_YELLOW);
            printColoredLine("╚═════════════════════════════════════════════════════════════╝",
                    ConsoleColor.BRIGHT_YELLOW);
            printLine("");
            printColoredLine("Available Points: " + ConsoleColor.BRIGHT_YELLOW + player.getUnspentStatPoints()
                    + ConsoleColor.RESET, ConsoleColor.BRIGHT_WHITE);
            printLine("");

            printLine("╔═══════════════════════════════════════════════════════════╗");
            printLine("║  CURRENT STATS                                            ║");
            printLine("╠═══════════════════════════════════════════════════════════╣");
            printColoredLine(
                    String.format("║  [1] Strength     : %-2d  (Increases damage)            ║", player.getStrength()),
                    ConsoleColor.BRIGHT_RED);
            printColoredLine(String.format("║  [2] Resilience   : %-2d (Reduces damage taken)           ║",
                    player.getResilience()), ConsoleColor.BRIGHT_GREEN);
            printColoredLine(
                    String.format("║  [3] Speed        : %-2d  (Affects turn order)         ║", player.getSpeed()),
                    ConsoleColor.BRIGHT_CYAN);
            printColoredLine(String.format("║  [4] Intelligence : %-2d  (Affects magic)               ║",
                    player.getIntelligence()), ConsoleColor.BRIGHT_MAGENTA);
            printColoredLine(
                    String.format("║  [5] Luck         : %-2d  (Affects loot drops)        ║", player.getLuck()),
                    ConsoleColor.BRIGHT_GREEN);
            printColoredLine("╠═══════════════════════════════════════════════════════════╣", ConsoleColor.BRIGHT_CYAN);
            printColoredLine(String.format("║  Unspent Points: %-42d ║", player.getUnspentStatPoints()),
                    ConsoleColor.BRIGHT_YELLOW);
            printColoredLine("╚═══════════════════════════════════════════════════════════╝", ConsoleColor.BRIGHT_CYAN);
            printLine("");
            printColoredLine("Strength = harder hits | Resilience = less damage taken", ConsoleColor.BRIGHT_RED);
            printColoredLine("Speed = earlier turns/first strike | Intelligence = puzzles/magic scaling",
                    ConsoleColor.BRIGHT_CYAN);
            printColoredLine("Luck = crit chance & better loot", ConsoleColor.BRIGHT_GREEN);

            if (player.getUnspentStatPoints() > 0) {
                printLine("Enter stat to increase (strength/str, resilience/res, speed/spd,");
                printLine("                       intelligence/int, luck/lck)");
                printLine("Or type 'done' to finish allocation");
                print("> ");

                String input = readLine().trim();

                if (input.equalsIgnoreCase("done") || input.equalsIgnoreCase("exit")) {
                    break;
                }

                if (player.spendStatPoint(input)) {
                    showSuccess("Increased " + input + "!");
                } else {
                    showError("Invalid stat name. Use: strength, resilience, speed, intelligence, or luck");
                }
            }
        }

        // ASCII is really hard to get right, and not all consoles support all
        // characters
        // And spacing is also an issue
        // So if there are issues, so be it - I'm tired of fighting with it

        if (player.getUnspentStatPoints() == 0) {
            showSuccess("All stat points allocated!");
        } else {
            showInfo("You can allocate remaining points later.");
        }

        pressAnyKey();
        return true;
    }

    public void printCombatHeader(Player player, Monster monster) {
        combatView.printCombatHeader(player, monster);
    }

    public void showCombatState(Player player, Monster monster, boolean isPlayerTurn, String lastAction) {
        combatView.showCombatState(player, monster, isPlayerTurn, lastAction);
    }

    public void showCombatState(Player player, Monster monster, boolean isPlayerTurn, String lastAction,
            int comboCount, boolean playerFocused, int playerBleed, int playerPoison,
            int monsterBleed, int monsterPoison) {
        combatView.showCombatState(player, monster, isPlayerTurn, lastAction,
                comboCount, playerFocused, playerBleed, playerPoison,
                monsterBleed, monsterPoison);
    }

    // WIP screen calibration wizard
    // I was inspired by friend who used a similar feature in their game
    public void calibrateScreenSize(GameSettings settings) {
        clear();
        printCentered(ConsoleColor.BRIGHT_CYAN + "╔════════════════════════════════════════╗" + ConsoleColor.RESET);
        printCentered(ConsoleColor.BRIGHT_CYAN + "║   SCREEN SIZE CALIBRATION WIZARD      ║" + ConsoleColor.RESET);
        printCentered(ConsoleColor.BRIGHT_CYAN + "╚════════════════════════════════════════╝" + ConsoleColor.RESET);
        printLine("");
        printLine("This wizard will help you configure the game to fit your console window.");
        printLine("We'll show you a test screen and you can adjust until it fits perfectly.");
        printLine("");
        pressAnyKey();

        boolean calibrating = true;
        int testViewportWidth = settings.viewportWidth;
        int testViewportHeight = settings.viewportHeight;

        while (calibrating) {
            clear();
            showTestScreen(testViewportWidth, testViewportHeight);

            printLine("");
            printLine(ConsoleColor.BRIGHT_YELLOW + "Does everything fit on your screen?" + ConsoleColor.RESET);
            printLine("  [Y] Yes, looks perfect!");
            printLine("  [T] Too large (doesn't fit)");
            printLine("  [S] Too small (lots of empty space)");
            printLine("  [M] Manual entry");
            printLine("");
            print("Choice > ");

            String choice = readLine().trim().toUpperCase();

            switch (choice) {
                case "Y" -> {
                    settings.viewportWidth = testViewportWidth;
                    settings.viewportHeight = testViewportHeight;
                    settings.consoleWidth = testViewportWidth + 10; // Add padding for borders
                    settings.consoleHeight = testViewportHeight + 12; // Add padding for HUD

                    // Save the settings to file for persistence
                    if (settings.saveScreenSettings()) {
                        showSuccess(
                                "Screen size configured and saved: " + testViewportWidth + "x" + testViewportHeight);
                    } else {
                        showSuccess("Screen size configured: " + testViewportWidth + "x" + testViewportHeight);
                        showWarning("(Note: Could not save settings to file)");
                    }
                    pressAnyKey();
                    calibrating = false;
                }

                case "T" -> {
                    // Reduce by 20%
                    testViewportWidth = Math.max(40, (int) (testViewportWidth * 0.8));
                    testViewportHeight = Math.max(15, (int) (testViewportHeight * 0.8));
                }

                case "S" -> {
                    // Increase by 20%
                    testViewportWidth = Math.min(120, (int) (testViewportWidth * 1.2));
                    testViewportHeight = Math.min(60, (int) (testViewportHeight * 1.2));
                }

                case "M" -> {
                    clear();
                    printLine(ConsoleColor.BRIGHT_CYAN + "Manual Configuration" + ConsoleColor.RESET);
                    printLine("");
                    print("Enter viewport width (40-120): ");
                    try {
                        int w = Integer.parseInt(readLine().trim());
                        if (w >= 40 && w <= 120) {
                            testViewportWidth = w;
                        } else {
                            showError("Width must be between 40 and 120");
                            pressAnyKey();
                        }
                    } catch (NumberFormatException e) {
                        showError("Invalid number");
                        pressAnyKey();
                    }

                    print("Enter viewport height (15-60): ");
                    try {
                        int h = Integer.parseInt(readLine().trim());
                        if (h >= 15 && h <= 60) {
                            testViewportHeight = h;
                        } else {
                            showError("Height must be between 15 and 60");
                            pressAnyKey();
                        }
                    } catch (NumberFormatException e) {
                        showError("Invalid number");
                        pressAnyKey();
                    }
                }

                default -> {
                    showError("Invalid choice");
                    pressAnyKey();
                }
            }
        }
    }

    // Test screen
    private void showTestScreen(int viewportWidth, int viewportHeight) {
        String playerName = "ADVENTURER";
        int hudWidth = Math.max(viewportWidth, 80);

        printLine(ConsoleColor.BRIGHT_CYAN + "┌─ " + playerName + " " + "─".repeat(hudWidth - playerName.length() - 4)
                + "┐" + ConsoleColor.RESET);
        printLine(ConsoleColor.BRIGHT_CYAN + "│ " + ConsoleColor.RESET +
                String.format("%-40s Level: 1   │ Floor: 1     ", "Name1") +
                " ".repeat(Math.max(0, hudWidth - 62)) +
                ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        printLine(ConsoleColor.BRIGHT_CYAN + "│ " + ConsoleColor.RESET +
                "HP " + ConsoleColor.BRIGHT_RED + "[" + "█".repeat(30) + "]" + ConsoleColor.RESET + " 100/100" +
                " ".repeat(Math.max(0, hudWidth - 46)) +
                ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        printLine(ConsoleColor.BRIGHT_CYAN + "│ " + ConsoleColor.RESET +
                "ST " + ConsoleColor.BRIGHT_CYAN + "[" + "█".repeat(30) + "]" + ConsoleColor.RESET + " 100/100" +
                " ".repeat(Math.max(0, hudWidth - 46)) +
                ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        printLine(ConsoleColor.BRIGHT_CYAN + "│ " + ConsoleColor.RESET +
                String.format("Experience: 0      │ Score: 859%s", " ".repeat(Math.max(0, hudWidth - 43))) +
                ConsoleColor.BRIGHT_CYAN + "│" + ConsoleColor.RESET);
        printLine(ConsoleColor.BRIGHT_CYAN + "└" + "─".repeat(hudWidth) + "┘" + ConsoleColor.RESET);

        // Map viewport
        printLine(ConsoleColor.BRIGHT_CYAN + "┌" + "─".repeat(viewportWidth) + "┐" + ConsoleColor.RESET);

        for (int y = 0; y < viewportHeight; y++) {
            StringBuilder row = new StringBuilder();
            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);

            // Create a simple test pattern
            for (int x = 0; x < viewportWidth; x++) {
                if (x == viewportWidth / 2 && y == viewportHeight / 2) {
                    row.append(ConsoleColor.BRIGHT_YELLOW).append("@").append(ConsoleColor.RESET);
                } else if ((x + y) % 15 == 0) {
                    row.append(ConsoleColor.YELLOW).append("C").append(ConsoleColor.RESET);
                } else if ((x + y) % 17 == 0) {
                    row.append(ConsoleColor.GREEN).append("g").append(ConsoleColor.RESET);
                } else if (x % 20 == 0 || y % 8 == 0) {
                    row.append(ConsoleColor.WHITE).append("#").append(ConsoleColor.RESET);
                } else {
                    row.append(" ");
                }
            }

            row.append(ConsoleColor.BRIGHT_CYAN).append("│").append(ConsoleColor.RESET);
            printLine(row.toString());
        }

        printLine(ConsoleColor.BRIGHT_CYAN + "└" + "─".repeat(viewportWidth) + "┘" + ConsoleColor.RESET);
        printLine("");
        printLine(
                "[W/A/S/D] Move | [E] Interact | [I] Inventory | [1/3] Use Potion | [L] Lore | [C] Craft | [V] Save | [M] Map | [H] Help | [Q] Quit Floor");
    }
}
