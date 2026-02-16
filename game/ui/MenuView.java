// Jonathan Decondé - 3196362
package game.ui;

import game.GameSettings;
import util.ConsoleColor;

// MenuView handles most menu related UI stuff
public class MenuView {
    private final View baseView;
    private final GameSettings settings;

    public MenuView(View baseView, GameSettings settings) {
        this.baseView = baseView;
        this.settings = settings;
    }

    // Main menu
    public void showMainMenu() {
        baseView.clear();
        showTitle();
        baseView.printLine("");
        showMainMenuOptions();
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Enter choice" + ConsoleColor.RESET + " > ");
    }

    private void showMainMenuOptions() {
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  ╔═══════════════════════════════════════╗" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  ║" + ConsoleColor.RESET + "  What would you like to do?" + ConsoleColor.BRIGHT_CYAN + "       ║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  ╚═══════════════════════════════════════╝" + ConsoleColor.RESET);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [1]" + ConsoleColor.RESET + " " + ConsoleColor.BOLD + "Start New Adventure" + ConsoleColor.RESET);
        baseView.printLine("      Descend into the dungeon and seek glory");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [2]" + ConsoleColor.RESET + " " + ConsoleColor.BOLD + "Continue Journey" + ConsoleColor.RESET);
        baseView.printLine("      Resume your previous expedition");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [3]" + ConsoleColor.RESET + " " + ConsoleColor.BOLD + "Hall of Fame" + ConsoleColor.RESET);
        baseView.printLine("      View the greatest adventurers");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [4]" + ConsoleColor.RESET + " " + ConsoleColor.BOLD + "Preferences" + ConsoleColor.RESET);
        baseView.printLine("      Customize your experience");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [5]" + ConsoleColor.RESET + " " + ConsoleColor.BOLD + "Exit" + ConsoleColor.RESET);
        baseView.printLine("      Leave the dungeon");
        baseView.printLine("");
    }

    private void showTitle() {
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_RED + "    ╔═══════════════════════════════════════════════════════════╗" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED + "    ║" + ConsoleColor.BRIGHT_YELLOW + ConsoleColor.BOLD + "                                                               " + ConsoleColor.RESET + ConsoleColor.BRIGHT_RED + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED + "    ║" + ConsoleColor.BRIGHT_YELLOW + ConsoleColor.BOLD + "        DAEDALUS'S INFINITE DUNGEON                        " + ConsoleColor.RESET + ConsoleColor.BRIGHT_RED + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED + "    ║" + ConsoleColor.BRIGHT_YELLOW + ConsoleColor.BOLD + "                                                               " + ConsoleColor.RESET + ConsoleColor.BRIGHT_RED + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED + "    ╚═══════════════════════════════════════════════════════════╝" + ConsoleColor.RESET);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "    A procedurally generated dungeon crawl with turn-based combat" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "    Roguelike mechanics • Endless progression • Challenging bosses" + ConsoleColor.RESET);
    }

    // Difficulty selector
    public void showDifficultyMenu() {
        baseView.clear();
        showSectionHeader("CHOOSE YOUR CHALLENGE", ConsoleColor.BRIGHT_CYAN);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "Select difficulty to determine dungeon scaling and enemy strength:" + ConsoleColor.RESET);
        baseView.printLine("");

        String[][] difficulties = {
                { "Tutorial", "Harmless", "Perfect for beginners", ConsoleColor.BRIGHT_GREEN },
                { "Easy", "Lenient", "Forgiving & fun", ConsoleColor.GREEN },
                { "Normal", "Balanced", "Recommended for most", ConsoleColor.BRIGHT_CYAN },
                { "Hard", "Challenging", "For veterans", ConsoleColor.BRIGHT_YELLOW },
                { "Nightmare", "Brutal", "Few survive", ConsoleColor.BRIGHT_RED },
                { "Hell", "Impossible", "Masochist mode", ConsoleColor.RED }
        };

        for (int i = 0; i < difficulties.length; i++) {
            String color = difficulties[i][3];
            baseView.printLine("  " + color + "[" + (i + 1) + "]" + ConsoleColor.RESET + 
                    " " + String.format("%-12s", difficulties[i][0]) + 
                    " • " + ConsoleColor.BRIGHT_BLACK + difficulties[i][1] + ConsoleColor.RESET +
                    " • " + difficulties[i][2]);
        }

        baseView.printLine("");
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Your choice" + ConsoleColor.RESET + " > ");
    }

    // Settings menu
    public void showSettingsMenu() {
        baseView.clear();
        showSectionHeader("PREFERENCES", ConsoleColor.BRIGHT_GREEN);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "Customize your gameplay and visual experience:" + ConsoleColor.RESET);
        baseView.printLine("");

        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [1]" + ConsoleColor.RESET + " Gameplay Settings");
        baseView.printLine("      Bosses, leveling, events, puzzles & mimics");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [2]" + ConsoleColor.RESET + " Visual Settings");
        baseView.printLine("      Colors, emojis, ASCII art & screen size");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [3]" + ConsoleColor.RESET + " Calibrate Screen");
        baseView.printLine("      Adjust viewport dimensions for your terminal");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [4]" + ConsoleColor.RESET + " Debug Mode " +
                (settings.debugMode ? ConsoleColor.BRIGHT_GREEN + "[ENABLED]" : ConsoleColor.BRIGHT_RED + "[DISABLED]") +
                ConsoleColor.RESET);
        baseView.printLine("      Developer console for testing");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [5]" + ConsoleColor.RESET + " Back to Menu");
        baseView.printLine("");
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Your choice" + ConsoleColor.RESET + " > ");
    }

    public void showGameplaySettings() {
        baseView.clear();
        showSectionHeader("GAMEPLAY SETTINGS", ConsoleColor.BRIGHT_MAGENTA);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "Customize which game features are active:" + ConsoleColor.RESET);
        baseView.printLine("");
        
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [1]" + ConsoleColor.RESET + " Boss Encounters         " + getToggleStatus(settings.enableBosses));
        baseView.printLine("      Legendary foes appear every 5 floors");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [2]" + ConsoleColor.RESET + " Player Leveling         " + getToggleStatus(settings.enableLeveling));
        baseView.printLine("      Gain levels and allocate stat points");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [3]" + ConsoleColor.RESET + " Floor Events            " + getToggleStatus(settings.enableEvents));
        baseView.printLine("      Random environmental effects");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [4]" + ConsoleColor.RESET + " Mimic Chests            " + getToggleStatus(settings.enableMimicChests));
        baseView.printLine("      Treasure may contain living creatures");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [5]" + ConsoleColor.RESET + " Puzzle Encounters       " + getToggleStatus(settings.enablePuzzles));
        baseView.printLine("      Solve riddles for rewards");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [6]" + ConsoleColor.RESET + " Back to Preferences");
        baseView.printLine("");
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Your choice" + ConsoleColor.RESET + " > ");
    }

    public void showVisualSettings() {
        baseView.clear();
        showSectionHeader("VISUAL SETTINGS", ConsoleColor.BRIGHT_MAGENTA);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_BLACK + "Customize visual presentation and appearance:" + ConsoleColor.RESET);
        baseView.printLine("");
        
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [1]" + ConsoleColor.RESET + " ASCII Art               " + getToggleStatus(settings.enableAsciiArt));
        baseView.printLine("      Display decorative ASCII artwork");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [2]" + ConsoleColor.RESET + " Colored Text            " + getToggleStatus(settings.enableColors));
        baseView.printLine("      Use colors in the console interface");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [3]" + ConsoleColor.RESET + " Emoji Support           " + getToggleStatus(settings.enableEmojis));
        baseView.printLine("      Display emoji icons and symbols");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [4]" + ConsoleColor.RESET + " Screen Calibration");
        baseView.printLine("      Adjust viewport to your terminal size");
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "  [5]" + ConsoleColor.RESET + " Back to Preferences");
        baseView.printLine("");
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Your choice" + ConsoleColor.RESET + " > ");
    }

    private String getToggleStatus(boolean enabled) {
        return enabled ? ConsoleColor.BRIGHT_GREEN + "[ON]" : ConsoleColor.BRIGHT_RED + "[OFF]" + ConsoleColor.RESET;
    }

    // Saving and loading system
    public void showLoadGameMenu(String[] saveFiles) {
        baseView.clear();
        showSectionHeader("LOAD GAME", ConsoleColor.BRIGHT_CYAN);
        baseView.printLine("");
        baseView.printLine("Select a save file:");
        baseView.printLine("");

        for (int i = 0; i < saveFiles.length; i++) {
            baseView.printLine("  " + (i + 1) + ". " + saveFiles[i]);
        }

        baseView.printLine("");
        baseView.printLine("  0. Back to menu");
        baseView.printLine("");
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Choice" + ConsoleColor.RESET + ": ");
    }

    // Scoreboard
    public void showScoreboardHeader() {
        baseView.clear();
        showSectionHeader(" HALL OF FAME ", ConsoleColor.BRIGHT_YELLOW);
        baseView.printLine("");
    }

    public void showScoreboardEntry(int rank, String playerName, int score, int level, int experience, int kills) {
        String rankStr = String.format("%2d.", rank);
        String info = String.format(" %-20s │ Score: %8d │ Lv %2d │ Exp: %6d │ Kills: %3d",
                playerName, score, level, experience, kills);
        baseView.printLine(rankStr + info);
    }

    public void showNoScores() {
        baseView.printLine("  No scores recorded yet. Be the first to claim the leaderboard!");
    }

    // Helper methods and stuff

    // Header section drawer
    private void showSectionHeader(String title, String color) {
        int width = 80;
        int padding = (width - title.length()) / 2;

        baseView.printLine(color + "╔" + "═".repeat(width - 2) + "╗" + ConsoleColor.RESET);
        baseView.printLine(color + "║" + " ".repeat(padding - 1) +
                ConsoleColor.BOLD + title + ConsoleColor.RESET +
                color + " ".repeat(width - title.length() - padding - 1) + "║" + ConsoleColor.RESET);
        baseView.printLine(color + "╚" + "═".repeat(width - 2) + "╝" + ConsoleColor.RESET);
    }

    private void showMenuOptions(String[] options) {
        for (int i = 0; i < options.length; i++) {
            baseView.printLine("  " + ConsoleColor.BRIGHT_CYAN + "[" + (i + 1) + "]" +
                    ConsoleColor.RESET + " " + options[i]);
        }
        baseView.printLine("");
    }

    private void showToggleOptions(String[][] options) {
        for (int i = 0; i < options.length; i++) {
            String statusDisplay = options[i][1].isEmpty() ? ""
                    : ": " + (options[i][1].equals("ON") ? ConsoleColor.BRIGHT_GREEN + "ON"
                            : ConsoleColor.BRIGHT_RED + "OFF") + ConsoleColor.RESET;

            baseView.printLine("  " + ConsoleColor.BRIGHT_CYAN + "[" + (i + 1) + "]" +
                    ConsoleColor.RESET + " " + options[i][0] + statusDisplay);
        }
        baseView.printLine("");
    }
}
