// Jonathan Decondé - 3196362
package game;

import game.entities.Companion;
import game.entities.Player;
import game.entities.monsters.Monster;
import game.items.Item;
import game.items.ItemFactory;
import game.items.MonsterFactory;
import game.systems.AchievementSystem;
import game.systems.CraftingSystem;
import game.systems.DebugCommandSystem;
import game.systems.DungeonMasterSystem;
import game.systems.LeaderboardSystem;
import game.systems.LeaderboardSystem.LeaderboardEntry;
import game.systems.SaveSystem;
import game.systems.combat.CombatSystem;
import game.systems.dungeons.DungeonFloor;
import game.systems.dungeons.Room;
import game.systems.puzzles.SphinxPuzzle;
import game.systems.puzzles.TicTacToePuzzle;
import game.ui.View;
import java.util.Scanner;
import util.ConsoleColor;
import util.DynArray;
import util.Logger;
import util.RNG;

// The Game
// Manages just about everything
// Overly complicated
public class Game {
    // Core game state
    private Player player; // The player character
    private GameSettings settings; // Game configuration (difficulty, visual options, etc.)
    private View view; // UI/display system for rendering to console
    private RNG rng; // Random number generator for this run
    private DungeonFloor currentFloor; // The dungeon level the player is currently on
    private boolean running; // Whether the game loop should continue
    private Scanner scanner; // For reading user input
    private Logger logger; // Debug/info logging system

    // Game systems
    private SaveSystem saveSystem; // Handles saving/loading game state
    private AchievementSystem achievementSystem; // Tracks and unlocks achievements
    private DebugCommandSystem debugSystem; // Debug console for testing (only in debug mode)
    private DungeonMasterSystem dungeonMaster; // Orchestrates dungeon generation and difficulty scaling

    // Game progression
    private long runSeed; // The seed for this entire run (ensures dungeon generation consistency)
    private int currentFloorNumber; // Current floor level (increments each time player reaches the exit)
    private String lastActionMessage; // Temporary message displayed to player (cleared after rendering)
    private String floorEvent; // Current floor event (darkness, tremors, calm, etc.)

    // Constants for game balancing (extracted from magic numbers)
    private static final ItemType[] ARMOR_SLOTS = {
            ItemType.ARMOR_HELMET,
            ItemType.ARMOR_CHESTPLATE,
            ItemType.ARMOR_LEGGINGS,
            ItemType.ARMOR_BOOTS
    };
    private static final int TRAP_BASE_DAMAGE = 5;
    private static final int TEMPLE_COST_BASE = 150;
    private static final int TEMPLE_COST_PER_FLOOR = 20;
    private static final int TEMPLE_GEAR_COST_OFFSET = 30;
    private static final int TREASURE_GOLD_BASE = 50;
    private static final int MAP_FRAGMENT_CHANCE = 30;
    private static final int SAVE_QUIT_SCORE_COST = 100;
    private static final int FLOOR_GENERATION_MAX_RETRIES = 5;

    // Additional constants for better maintainability
    private static final int DEFAULT_PLAYER_NAME_MAX_LENGTH = 30;
    private static final String DEFAULT_PLAYER_NAME = "Adventurer";
    private static final double STAMINA_REGEN_RATE = 0.10; // 10% per action
    private static final int INTERACT_POTION_CHANCE = 30;
    private static final int INTERACT_GOLD_CHANCE = 50;

    // Safety & Prevention Constants
    private static final int MAX_INVENTORY_SIZE = 100; // Prevent memory issues
    private static final int AUTO_SAVE_INTERVAL = 1; // Auto-save every floor
    private static final int MAX_FAILED_INPUTS = 3; // After 3 bad inputs, show help
    private static final int MIN_SAFE_HP_PERCENT = 15; // Warn if HP below 15%

    // Safety tracking variables
    private int consecutiveFailedInputs = 0;
    private int floorsCompletedSinceLastSave = 0;
    private static final int INTERACT_GOLD_MIN = 20;
    private static final int BOSS_ROAR_BASE_REVEAL_DURATION = 2;
    private static final int BOSS_ROAR_INTELLIGENCE_DIVISOR = 5;
    private static final int BOSS_ROAR_MAX_INTELLIGENCE_BONUS = 8;
    private static final int BOSS_ROAR_MAX_REVEAL_DURATION = 10;
    private static final int BOSS_MOVE_FREQUENCY = 3;

    // Treasure & Lore constants
    private static final int LORE_ITEM_SCORE_REWARD = 200;
    private static final int TREASURE_GOLD_MULTIPLIER = 100;
    private static final int MAP_FRAGMENT_REVEAL_ROOMS = 3;

    // Command constants
    private static final String MSG_INVALID_CHOICE = "Invalid choice!";
    private static final String MSG_ENTER_VALID_NUMBER = "Please enter a valid number!";
    private static final String CMD_QUIT = "Q";
    private static final String CMD_HELP = "H";
    private static final String CMD_MAP = "M";
    private static final String CMD_SAVE = "V";
    private static final String CMD_CRAFT = "C";
    private static final String CMD_LORE = "L";
    private static final String CMD_INVENTORY = "I";
    private static final String CMD_INTERACT = "E";

    public Game() {
        this(false); // Default to no debug mode
    }

    public Game(boolean debugMode) {
        this.logger = Logger.getInstance();
        this.settings = new GameSettings();
        this.settings.debugMode = debugMode;

        // Load saved screen settings if they exist
        if (settings.loadScreenSettings()) {
            logger.info("GAME", "Loaded saved screen settings: " +
                    settings.viewportWidth + "x" + settings.viewportHeight);
        }

        // Initialize logger with settings
        this.logger.initialize(this.settings.debugMode, false); // Turn to on to get lovely debug logs
        this.logger.info("GAME", "Game instance created with debug mode: " + debugMode);

        this.view = new View(settings.enableColors, settings.enableEmojis);
        this.view.initializeViews(settings); // Initialize specialized views

        // Apply viewport settings to MapView
        this.view.getMapView().setViewportSize(settings.viewportWidth, settings.viewportHeight);

        this.rng = new RNG();
        this.dungeonMaster = new DungeonMasterSystem();
        this.runSeed = rng.nextLong();
        if (this.runSeed == 0L) {
            logger.warn("GAME", "Generated run seed was zero; regenerating");
            this.runSeed = rng.nextLong();
        }
        this.scanner = new Scanner(System.in);
        this.running = false;
        this.saveSystem = new SaveSystem();
        this.achievementSystem = new AchievementSystem();
        this.currentFloorNumber = 1;
        this.lastActionMessage = "";

        this.logger.info("GAME", "Game initialization complete");
    }

    public void start() {
        logger.info("GAME", "Starting game");
        try {
            running = true;
            mainMenu();
        } catch (Exception e) {
            logger.error("GAME", "Fatal error in game start", e);
            view.showError("Fatal error: " + e.getMessage());
        } finally {
            logger.info("GAME", "Game stopped");
            if (scanner != null) {
                scanner.close();
            }
            logger.close();
        }
    }

    private void mainMenu() {
        logger.info("MENU", "Entering main menu");
        while (running) {
            try {
                view.clear();
                view.showMainMenu();

                String choice = view.readLine().trim();
                logger.debug("MENU", "User selected: " + choice);

                switch (choice) {
                    case "1" -> {
                        resetInputFailureCounter();
                        newGame();
                    }
                    case "2" -> {
                        resetInputFailureCounter();
                        continueGame();
                    }
                    case "3" -> {
                        resetInputFailureCounter();
                        showScoreboard();
                    }
                    case "4" -> {
                        resetInputFailureCounter();
                        showSettings();
                    }
                    case "5" -> {
                        resetInputFailureCounter();
                        running = false;
                        view.printLine("Goodbye!");
                    }
                    default -> handleInvalidInput(choice);
                }
            } catch (NumberFormatException e) {
                handleInvalidInput("<number>");
                logger.debug("MENU", "Invalid menu input");
            } catch (Exception e) {
                view.showError("Input error: " + e.getMessage());
                logger.error("MENU", "Error in main menu", e);
            }
        }
        // Scanner remains open until program exit; closed in start() finally block
    }

    private void newGame() {
        logger.info("GAME", "Starting new game");
        try {
            view.clear();
            view.printCentered("CREATE NEW CHARACTER");
            view.printLine("");
            view.print("Enter your name (1-" + DEFAULT_PLAYER_NAME_MAX_LENGTH + " characters): ");
            String name = view.readLine().trim();
            logger.debug("GAME", "Player name input: " + name);

            // Validate player name length
            name = validatePlayerName(name);

            this.player = new Player(name);
            player.addCompanion(new Companion("Aerin", Companion.Role.ROGUE, ElementType.AIR, player.getLevel()));
            player.addCompanion(new Companion("Lyra", Companion.Role.HEALER, ElementType.LIGHT, player.getLevel()));
            this.currentFloorNumber = 1;
            this.debugSystem = new DebugCommandSystem(player, view, settings.debugMode);
            logger.info("GAME", "Player created: " + name);

            view.clear();
            view.showDifficultyMenu();

            try {
                String diffChoice = view.readLine().trim();
                if (diffChoice.isEmpty()) {
                    logger.warn("GAME", "Empty difficulty selection, defaulting to NORMAL");
                    this.settings.difficulty = Difficulty.NORMAL;
                    resetInputFailureCounter();
                } else {
                    Difficulty difficulty = switch (diffChoice) {
                        case "1" -> Difficulty.TUTORIAL;
                        case "2" -> Difficulty.EASY;
                        case "3" -> Difficulty.NORMAL;
                        case "4" -> Difficulty.HARD;
                        case "5" -> Difficulty.NIGHTMARE;
                        case "6" -> Difficulty.HELL;
                        default -> {
                            logger.warn("GAME", "Invalid difficulty choice: " + diffChoice);
                            handleInvalidInput(diffChoice);
                            yield Difficulty.NORMAL;
                        }
                    };
                    this.settings.difficulty = difficulty;
                    logger.info("GAME", "Difficulty set to: " + difficulty.displayName);
                    resetInputFailureCounter();
                }
            } catch (NumberFormatException ex) {
                handleInvalidInput("<number>");
                logger.warn("GAME", "Invalid difficulty input, defaulting to NORMAL");
                this.settings.difficulty = Difficulty.NORMAL;
            } catch (Exception ex) {
                logger.error("GAME", "Error setting difficulty", ex);
                this.settings.difficulty = Difficulty.NORMAL;
            }

            view.printColoredLine("Run Seed: " + runSeed, ConsoleColor.BRIGHT_CYAN);
            view.printLine("");

            // Starter gear based on difficulty
            ItemFactory.giveStarterGear(settings.difficulty, rng, item -> {
                boolean added = player.addItem(item);
                if (!added) {
                    logger.warn("GAME", "Starter gear dropped due to full inventory: " + item.getName());
                    return;
                }
                if (item.getType().isWeapon()) {
                    player.equipWeapon(item);
                } else if (item.getType().isArmor()) {
                    player.equipArmor(item);
                }
            });

            // Show intro cutscene
            showIntroCutscene();

            gameLoop();
        } catch (Exception e) {
            logger.error("GAME", "Error in new game creation", e);
            view.showError("Error creating new game: " + e.getMessage());
            view.pressAnyKey();
        }
    }

    private void continueGame() {
        logger.info("MENU", "Loading saved game");

        DynArray<String> saveFiles = saveSystem.listSaveFiles();

        if (saveFiles.size() == 0) {
            view.showWarning("No saved games found!");
            view.pressAnyKey();
            return;
        }

        view.clear();
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "LOAD GAME" + ConsoleColor.RESET);
        view.printLine("");
        view.printLine("Select a save file:");
        view.printLine("");

        for (int i = 0; i < saveFiles.size(); i++) {
            view.printLine("  " + (i + 1) + ". " + saveFiles.get(i));
        }
        view.printLine("");
        view.printLine("  0. Back to menu");
        view.printLine("");
        view.print("Choice: ");

        try {
            String input = view.readLine().trim();

            // Validate empty input
            if (input.isEmpty()) {
                view.showWarning("Please enter a valid choice!");
                logger.debug("LOAD", "Empty choice input");
                view.pressAnyKey();
                continueGame();
                return;
            }

            int choice = parseIntOrDefault(input, -1);

            if (choice == 0) {
                logger.info("LOAD", "User cancelled load game");
                return;
            }

            if (!isValidChoice(choice, 1, saveFiles.size())) {
                view.showError("Invalid choice! Please select 1-" + saveFiles.size());
                logger.debug("LOAD", "Out of range choice: " + choice);
                view.pressAnyKey();
                continueGame();
                return;
            }

            String filename = saveFiles.get(choice - 1);
            SaveSystem.SaveData data = saveSystem.loadGame(filename);

            if (data == null) {
                view.showError("Failed to load save file!");
                view.pressAnyKey();
                return;
            }

            // Create player and restore all saved data
            player = new Player(data.playerName);
            player.loadFromSaveData(
                    data.level, data.experience,
                    data.health, data.maxHealth,
                    data.stamina, data.maxStamina,
                    data.strength, data.resilience, data.speed, data.intelligence, data.luck, data.unspentStatPoints,
                    data.floorReached, data.totalScore,
                    data.monstersDefeated, data.puzzlesSolved, data.mimicsDefeated,
                    data.itemsCrafted, data.chestsOpened);

            if (data.runSeed != 0L) {
                this.runSeed = data.runSeed;
            } else {
                logger.warn("SAVE", "Run seed missing or zero; generating new seed");
                this.runSeed = rng.nextLong();
            }

            // Reconstruct equipped items from saved data
            // Items are fully deserialized preserving rarity, stats, traits, etc.
            if (data.equippedWeapon != null && !data.equippedWeapon.isEmpty()) {
                Item weapon = Item.deserialize(data.equippedWeapon);
                if (weapon != null) {
                    player.equipWeapon(weapon);
                    logger.info("SAVE", "Equipped weapon: " + weapon.getName());
                } else {
                    logger.warn("SAVE", "Failed to deserialize weapon: " + data.equippedWeapon);
                }
            }

            // Check for armor equipment (note: gauntlets were removed from system)
            if (data.equippedHelmet != null && !data.equippedHelmet.isEmpty()) {
                Item helmet = Item.deserialize(data.equippedHelmet);
                if (helmet != null) {
                    player.equipArmor(helmet);
                    logger.info("SAVE", "Equipped helmet: " + helmet.getName());
                }
            }
            if (data.equippedChestplate != null && !data.equippedChestplate.isEmpty()) {
                Item chestplate = Item.deserialize(data.equippedChestplate);
                if (chestplate != null) {
                    player.equipArmor(chestplate);
                    logger.info("SAVE", "Equipped chestplate: " + chestplate.getName());
                }
            }
            if (data.equippedLeggings != null && !data.equippedLeggings.isEmpty()) {
                Item leggings = Item.deserialize(data.equippedLeggings);
                if (leggings != null) {
                    player.equipArmor(leggings);
                    logger.info("SAVE", "Equipped leggings: " + leggings.getName());
                }
            }
            if (data.equippedBoots != null && !data.equippedBoots.isEmpty()) {
                Item boots = Item.deserialize(data.equippedBoots);
                if (boots != null) {
                    player.equipArmor(boots);
                    logger.info("SAVE", "Equipped boots: " + boots.getName());
                }
            }

            // Restore inventory items from saved data
            // Items are fully deserialized with all stats, rarity, traits preserved
            int skippedItems = 0;
            for (int i = 0; i < data.inventoryItems.size(); i++) {
                String savedItemData = data.inventoryItems.get(i);
                if (savedItemData != null && !savedItemData.isEmpty()) {
                    // Try to deserialize new format first
                    Item item = Item.deserialize(savedItemData);
                    if (item != null) {
                        boolean added = player.addItem(item);
                        if (!added) {
                            skippedItems++;
                            logger.warn("SAVE", "Inventory full while restoring item: " + item.getName());
                        }
                    } else {
                        // Fallback for old save format (just names)
                        // Check if it's a mana potion and convert to stamina
                        if (savedItemData.toLowerCase().contains("mana potion")) {
                            Item stamina = new Item("Stamina Potion", ItemType.POTION_STAMINA, ElementType.NEUTRAL,
                                    Rarity.COMMON);
                            if (!player.addItem(stamina)) {
                                skippedItems++;
                                logger.warn("SAVE", "Inventory full while restoring potion");
                            }
                            logger.info("SAVE", "Converted Mana Potion to Stamina Potion");
                        } else {
                            logger.warn("SAVE", "Could not deserialize item: " + savedItemData);
                            skippedItems++;
                        }
                    }
                }
            }

            if (skippedItems > 0) {
                logger.warn("SAVE", "Failed to restore " + skippedItems + " item(s) from save");
                lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "⚠ Restored inventory with " + skippedItems
                        + " item(s) lost" + ConsoleColor.RESET;
            } else {
                lastActionMessage = ConsoleColor.BRIGHT_GREEN + "✓ Inventory fully restored" + ConsoleColor.RESET;
            }

            // I didn't think this through....

            currentFloorNumber = data.currentFloor;

            // Initialize debug system with loaded player
            this.debugSystem = new DebugCommandSystem(player, view, settings.debugMode);

            view.showSuccess("Game loaded successfully!");
            view.printLine("Resuming from floor " + currentFloorNumber);
            view.pressAnyKey();

            gameLoop();

        } catch (NumberFormatException e) {
            view.showError("Please enter a valid number!");
            logger.debug("LOAD", "Invalid numeric input");
            view.pressAnyKey();
            continueGame();
        } catch (NullPointerException e) {
            logger.error("LOAD", "Corrupt save file data", e);
            view.showError("Save file is corrupted and cannot be loaded.");
            view.pressAnyKey();
            continueGame();
        } catch (Exception e) {
            logger.error("LOAD", "Error loading game", e);
            view.showError("Error loading game: " + e.getMessage());
            view.pressAnyKey();
            continueGame();
        }
    }

    private void showScoreboard() {
        view.clear();
        view.printCentered(ConsoleColor.BRIGHT_YELLOW + "HIGH SCORES" + ConsoleColor.RESET);
        view.printLine("");

        DynArray<LeaderboardEntry> entries = LeaderboardSystem.loadLeaderboard();

        if (entries.size() == 0) {
            view.printLine("  No scores recorded yet. Be the first to claim the leaderboard!");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                String rank = String.format("%2d.", i + 1);
                String info = String.format(" %-20s | Score: %8d | Lv %2d | Exp: %6d | Kills: %3d | Seed: %d",
                    entry.playerName, entry.score, entry.level, entry.experience, entry.monstersDefeated,
                    entry.runSeed);
                view.printLine(rank + info);
            }
        }

        view.printLine("");
        view.pressAnyKey();
    }

    private void showSettings() {
        view.clear();
        view.showSettingsMenu();

        try {
            String choice = view.readLine().trim();

            switch (choice) {
                case "1" -> {
                    showGameplaySettings();
                    resetInputFailureCounter();
                }
                case "2" -> {
                    showVisualSettings();
                    resetInputFailureCounter();
                }
                case "3" -> {
                    toggleDebugMode();
                    resetInputFailureCounter();
                }
                case "4" -> resetInputFailureCounter();
                default -> {
                    view.showError("Invalid choice!");
                    handleInvalidInput(choice);
                }
            }
        } catch (Exception e) {
            view.showError("Input error");
            handleInvalidInput("");
        }
    }

    private void showGameplaySettings() {
        boolean inSettings = true;
        while (inSettings) {
            view.clear();
            view.printCentered(
                    ConsoleColor.BRIGHT_MAGENTA + "═══════════════════════════════════════" + ConsoleColor.RESET);
            view.printCentered(ConsoleColor.BRIGHT_CYAN + "GAMEPLAY SETTINGS" + ConsoleColor.RESET);
            view.printCentered(
                    ConsoleColor.BRIGHT_MAGENTA + "═══════════════════════════════════════" + ConsoleColor.RESET);
            view.printLine("");
            view.printLine(ConsoleColor.BRIGHT_YELLOW + "  COMBAT & ENCOUNTERS" + ConsoleColor.RESET);
            view.printLine("  [1] Boss Encounters: "
                    + (settings.enableBosses ? ConsoleColor.BRIGHT_GREEN + "ON" : ConsoleColor.BRIGHT_RED + "OFF")
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_BLACK + "      → Legendary foes every 5 floors" + ConsoleColor.RESET);
            view.printLine("");
            view.printLine("  [2] Mimic Chests: "
                    + (settings.enableMimicChests ? ConsoleColor.BRIGHT_GREEN + "ON" : ConsoleColor.BRIGHT_RED + "OFF")
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_BLACK + "      → Treasure may be alive!" + ConsoleColor.RESET);
            view.printLine("");
            view.printLine(ConsoleColor.BRIGHT_YELLOW + "  PROGRESSION & CHALLENGES" + ConsoleColor.RESET);
            view.printLine("  [3] Player Leveling: "
                    + (settings.enableLeveling ? ConsoleColor.BRIGHT_GREEN + "ON" : ConsoleColor.BRIGHT_RED + "OFF")
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_BLACK + "      → Gain levels & allocate stats" + ConsoleColor.RESET);
            view.printLine("");
            view.printLine("  [4] Puzzle Encounters: "
                    + (settings.enablePuzzles ? ConsoleColor.BRIGHT_GREEN + "ON" : ConsoleColor.BRIGHT_RED + "OFF")
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_BLACK + "      → Riddles, Tic-Tac-Toe, etc." + ConsoleColor.RESET);
            view.printLine("");
            view.printLine("  [5] Floor Events: "
                    + (settings.enableEvents ? ConsoleColor.BRIGHT_GREEN + "ON" : ConsoleColor.BRIGHT_RED + "OFF")
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_BLACK + "      → Random environmental effects" + ConsoleColor.RESET);
            view.printLine("");
            view.printLine(ConsoleColor.BRIGHT_YELLOW + "  DUNGEON CONFIGURATION" + ConsoleColor.RESET);
            view.printLine("  [6] Dungeon Size: " + ConsoleColor.BRIGHT_CYAN + settings.mapWidth + "x"
                    + settings.mapHeight + ConsoleColor.RESET);
            view.printLine(
                    ConsoleColor.BRIGHT_BLACK + "      → Current: " + getDungeonSizeDescription() + ConsoleColor.RESET);
            view.printLine("");
            view.printLine(ConsoleColor.BRIGHT_MAGENTA + "  [S] Save Settings to Config" + ConsoleColor.RESET);
            view.printLine("  [B] Back");
            view.printLine("");
            view.print(ConsoleColor.BRIGHT_YELLOW + "Choice" + ConsoleColor.RESET + " > ");

            try {
                String choice = view.readLine().trim().toUpperCase();

                switch (choice) {
                    case "1" -> settings.enableBosses = !settings.enableBosses;
                    case "2" -> settings.enableMimicChests = !settings.enableMimicChests;
                    case "3" -> settings.enableLeveling = !settings.enableLeveling;
                    case "4" -> settings.enablePuzzles = !settings.enablePuzzles;
                    case "5" -> settings.enableEvents = !settings.enableEvents;
                    case "6" -> configureDungeonSize();
                    case "S" -> {
                        if (settings.saveScreenSettings()) {
                            view.showSuccess("Settings saved to saves/screen_settings.cfg");
                        } else {
                            view.showError("Failed to save settings!");
                        }
                        view.pressAnyKey();
                    }
                    case "B" -> inSettings = false;
                    default -> view.showError("Invalid choice!");
                }
            } catch (Exception e) {
                view.showError("Input error");
            }
        }
    }

    private String getDungeonSizeDescription() {
        int totalSize = settings.mapWidth * settings.mapHeight;
        if (totalSize < 4000)
            return "Tiny (Quick)";
        if (totalSize < 5500)
            return "Small (Fast)";
        if (totalSize < 7500)
            return "Medium (Balanced)";
        if (totalSize < 10000)
            return "Large (Exploration)";
        return "Huge (Epic)";
    }

    private void configureDungeonSize() {
        view.clear();
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "DUNGEON SIZE CONFIGURATION" + ConsoleColor.RESET);
        view.printLine("");
        view.printLine("Choose dungeon size preset:");
        view.printLine("");
        view.printLine("  [1] Tiny    (60x40)   - Quick runs, fast exploration");
        view.printLine("  [2] Small   (75x50)   - Compact, beginner-friendly");
        view.printLine("  [3] Medium  (90x55)   - " + ConsoleColor.BRIGHT_GREEN + "[CURRENT DEFAULT]"
                + ConsoleColor.RESET + " Balanced");
        view.printLine("  [4] Large   (120x75)  - More rooms, longer gameplay");
        view.printLine("  [5] Huge    (150x95)  - Epic exploration, for veterans");
        view.printLine("  [6] Custom  - Enter your own dimensions");
        view.printLine("  [B] Back");
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_YELLOW + "Current: " + settings.mapWidth + "x" + settings.mapHeight
                + ConsoleColor.RESET);
        view.printLine("");
        view.print("Choice > ");

        try {
            String choice = view.readLine().trim().toUpperCase();
            switch (choice) {
                case "1" -> {
                    settings.mapWidth = 60;
                    settings.mapHeight = 40;
                }
                case "2" -> {
                    settings.mapWidth = 75;
                    settings.mapHeight = 50;
                }
                case "3" -> {
                    settings.mapWidth = 90;
                    settings.mapHeight = 55;
                }
                case "4" -> {
                    settings.mapWidth = 120;
                    settings.mapHeight = 75;
                }
                case "5" -> {
                    settings.mapWidth = 150;
                    settings.mapHeight = 95;
                }
                case "6" -> {
                    view.printLine("");
                    view.print("Enter width (50-200): ");
                    int width = Integer.parseInt(view.readLine().trim());
                    view.print("Enter height (30-120): ");
                    int height = Integer.parseInt(view.readLine().trim());
                    settings.mapWidth = Math.max(50, Math.min(200, width));
                    settings.mapHeight = Math.max(30, Math.min(120, height));
                }
                case "B" -> {
                    return;
                }
                default -> {
                    view.showError("Invalid choice!");
                    view.pressAnyKey();
                    return;
                }
            }
            view.showSuccess("Dungeon size set to " + settings.mapWidth + "x" + settings.mapHeight + " ("
                    + getDungeonSizeDescription() + ")");
            view.printLine(
                    ConsoleColor.BRIGHT_YELLOW + "Note: Changes take effect on new floors/games" + ConsoleColor.RESET);
            view.pressAnyKey();
        } catch (NumberFormatException e) {
            view.showError("Invalid number format!");
            view.pressAnyKey();
        } catch (Exception e) {
            view.showError("Input error");
        }
    }

    private void showVisualSettings() {
        boolean inSettings = true;
        while (inSettings) {
            view.clear();
            view.printCentered("VISUAL SETTINGS");
            view.printLine("");
            view.printLine("  [1] Toggle ASCII Art: " + (settings.enableAsciiArt ? "ON" : "OFF"));
            view.printLine("  [2] Toggle Colors: " + (settings.enableColors ? "ON" : "OFF"));
            view.printLine("  [3] Toggle Emojis: " + (settings.enableEmojis ? "ON" : "OFF"));
            view.printLine("  [4] Configure Screen Size");
            view.printLine("  [5] Back");
            view.printLine("");
            view.print("Choice > ");

            try {
                String choice = view.readLine().trim();

                switch (choice) {
                    case "1" -> {
                        settings.enableAsciiArt = !settings.enableAsciiArt;
                        // ASCII art setting is stored in GameSettings and used by specialized views
                    }
                    case "2" -> {
                        settings.enableColors = !settings.enableColors;
                        view.setUseColors(settings.enableColors);
                    }
                    case "3" -> {
                        settings.enableEmojis = !settings.enableEmojis;
                        view.setUseEmojis(settings.enableEmojis);
                    }
                    case "4" -> {
                        calibrateScreen();
                    }
                    case "5" -> {
                        inSettings = false;
                        resetInputFailureCounter();
                    }
                    default -> {
                        view.showError("Invalid choice!");
                        handleInvalidInput(choice);
                    }
                }
            } catch (Exception e) {
                view.showError("Input error");
                handleInvalidInput("");
            }
        }
    }

    private void toggleDebugMode() {
        settings.debugMode = !settings.debugMode;
        logger.initialize(settings.debugMode, true); // Logger always on with debug more
        if (debugSystem != null) {
            debugSystem.setEnabled(settings.debugMode);
        }
        if (settings.debugMode) {
            view.showSuccess("Debug mode: ON - Use '/' prefix for debug commands or type 'DEBUG CONSOLE'");
            logger.info("DEBUG", "Debug mode enabled");
        } else {
            view.showSuccess("Debug mode: OFF");
            logger.info("DEBUG", "Debug mode disabled");
        }
        view.pressAnyKey();
    }

    private void calibrateScreen() {
        view.calibrateScreenSize(settings);
        // Apply new viewport settings
        view.getMapView().setViewportSize(settings.viewportWidth, settings.viewportHeight);
        logger.info("SETTINGS", "Screen calibrated: " + settings.viewportWidth + "x" + settings.viewportHeight);
    }

    private void applyRandomFloorEvent() {
        // Floor events are random environmental effects that add variety and challenge
        // Each event has unique mechanics: vision changes, stat multipliers, or rewards
        // Events reroll each floor, creating unpredictable dungeon experiences

        floorEvent = "None";
        if (!settings.enableEvents) {
            return;
        }
        int roll = rng.nextInt(100);
        if (roll < 12) {
            // 12% chance: Darkness - greatly reduces vision
            floorEvent = "Darkness";
            currentFloor.setVisionRadius(2, 3);
            lastActionMessage = ConsoleColor.BRIGHT_BLACK
                    + "Absolute darkness consumes the floor. Vision nearly impossible." + ConsoleColor.RESET;
        } else if (roll < 22) {
            // 10% chance: Tremors - reveals layout but monsters become aggressive
            floorEvent = "Echoing Tremors";
            currentFloor.revealRandomRooms(3);
            player.setMonsterAggression(1.15); // 15% more aggressive monsters
            lastActionMessage = ConsoleColor.BRIGHT_YELLOW
                    + " The ground shakes violently! The disturbance agitates monsters nearby." + ConsoleColor.RESET;
        } else if (roll < 33) {
            // 11% chance: Miasma - toxic fog damages player but weakens monsters
            floorEvent = "Toxic Miasma";
            player.setMiasmaActive(true);
            lastActionMessage = ConsoleColor.BRIGHT_MAGENTA
                    + "☠ Noxious purple fog covers the floor, burning your lungs." + ConsoleColor.RESET;
        } else if (roll < 44) {
            // 11% chance: Ethereal Aura - weakens player defenses but boosts XP gain
            floorEvent = "Ethereal Aura";
            player.setAuraActive(true);
            lastActionMessage = ConsoleColor.BRIGHT_CYAN
                    + "A shimmering aura washes over you, making you feel exposed but strangely enlightened."
                    + ConsoleColor.RESET;
        } else if (roll < 55) {
            // 11% chance: Ancient Blessing - grants temporary combat advantages
            floorEvent = "Ancient Blessing";
            player.setBlessing(true);
            int healing = (int) (player.getMaxHealth() * 0.20);
            player.heal(healing);
            lastActionMessage = ConsoleColor.BRIGHT_GREEN
                    + "Divine energy infuses you! You feel rejuvenated and blessed." + ConsoleColor.RESET;
        } else if (roll < 66) {
            // 11% chance: Crystalline Tunnels - reveals treasure but hazardous
            floorEvent = "Crystalline Tunnels";
            currentFloor.revealRandomRooms(2);
            lastActionMessage = ConsoleColor.BRIGHT_WHITE
                    + "Glowing crystals illuminate passages, casting eerie reflections." + ConsoleColor.RESET;
        } else if (roll < 77) {
            // 11% chance: Mystic Veil - partially obscures rooms, creates mystery
            floorEvent = "Mystic Veil";
            currentFloor.setVisionRadius(4, 6);
            lastActionMessage = ConsoleColor.BRIGHT_MAGENTA
                    + "A shimmering veil shrouds the floor, obscuring all but basic shapes." + ConsoleColor.RESET;
        } else if (roll < 88) {
            // 11% chance: Spectral Convergence - monsters appear stronger
            floorEvent = "Spectral Convergence";
            player.setMonsterAggression(1.25);
            lastActionMessage = ConsoleColor.BRIGHT_MAGENTA
                    + "Ghostly apparitions swirl through the air. The veil between worlds is thin."
                    + ConsoleColor.RESET;
        } else {
            // 12% chance: Calm - nothing special
            floorEvent = "Calm";
            lastActionMessage = ConsoleColor.BRIGHT_GREEN + "☮ The air is still. A moment of peace before chaos."
                    + ConsoleColor.RESET;
        }
    }

    private void gameLoop() {
        // Main game loop: floors continue to be generated and explored until player
        // dies or quits
        // Each iteration: generates a new floor, applies events, lets player explore,
        // then advances
        // This is the core gameplay loop that drives progression through the dungeon

        logger.info("GAMELOOP", "Entering game loop");

        // If loading from save, start from saved floor number, otherwise start at 1
        if (currentFloorNumber == 0) {
            currentFloorNumber = 1;
        }

        while (player.isAlive() && running) {
            try {
                logger.info("GAMELOOP", "Generating floor " + currentFloorNumber);

                // Snapshot player stats before floor to detect progress for achievements
                int killsBefore = player.getMonstersDefeated();
                int chestsBefore = player.getChestsOpened();
                int puzzlesBefore = player.getPuzzlesSolved();

                // The DungeonMasterSystem determines how "tuned" this floor should be
                // (how many monsters, what difficulty, etc.) based on player level and
                // progression
                DungeonMasterSystem.Tuning tuning = dungeonMaster.planTuning(player);

                // Using the run seed + floor number ensures the same floor layout every time
                // (if you restart with the same seed, you get the same dungeon)
                long floorSeed = dungeonMaster.deriveFloorSeed(runSeed, currentFloorNumber);
                tuning.floorSeed = floorSeed;

                // Generate new floor with configured map dimensions - with retry limit to
                // prevent infinite loops
                int floorGenerationAttempts = 0;
                boolean floorGenerated = false;

                while (floorGenerationAttempts < FLOOR_GENERATION_MAX_RETRIES && !floorGenerated) {
                    currentFloor = new DungeonFloor(currentFloorNumber, settings.difficulty,
                            settings.mapWidth, settings.mapHeight, floorSeed, tuning, player.getLevel());
                    debugSystem.setCurrentFloor(currentFloor);
                    logger.debug("GAMELOOP", "Floor generated successfully");

                    // Validate floor (ensure it has proper rooms, connections, etc.)
                    if (currentFloor.isValid()) {
                        floorGenerated = true;
                    } else {
                        floorGenerationAttempts++;
                        logger.warn("GAMELOOP", "Floor validation failed (attempt " + floorGenerationAttempts + "/"
                                + FLOOR_GENERATION_MAX_RETRIES + "), retrying");
                        view.showWarning("Floor generation failed, retrying... (attempt " + floorGenerationAttempts
                                + "/" + FLOOR_GENERATION_MAX_RETRIES + ")");

                        // Use a different seed variant for retry
                        floorSeed = floorSeed + floorGenerationAttempts;
                        tuning.floorSeed = floorSeed;
                    }
                }

                // If floor generation failed after all retries, end the run
                if (!floorGenerated) {
                    logger.error("GAMELOOP",
                            "Floor generation failed after " + FLOOR_GENERATION_MAX_RETRIES + " attempts");
                    view.showError("FATAL: Unable to generate valid floor after " + FLOOR_GENERATION_MAX_RETRIES
                            + " attempts!");
                    running = false;
                    break;
                }

                // Display DM's current strategy to the player for flavor/immersion
                if (tuning.shouldShowDMMessage && !tuning.dmMessage.isEmpty()) {
                    view.printColoredLine("\n[DUNGEON MASTER] " + tuning.dmMessage, tuning.dmResponse.color);
                    view.printLine("");
                }

                // Apply a random environmental event to this floor
                applyRandomFloorEvent();

                logger.info("GAMELOOP", "Floor validated, starting exploration");
                // Let player explore the floor (move, fight, loot, etc.)
                exploreFloor();
                logger.debug("GAMELOOP", "Floor exploration complete");

                // If player died while exploring, end the game
                if (!player.isAlive()) {
                    break;
                }

                // Record how the player performed on this floor (for tracking stats)
                dungeonMaster.recordFloorResult(currentFloor, player, killsBefore, chestsBefore, puzzlesBefore);

                // Auto-save BEFORE advancing floor number (so we save the floor we just
                // completed, not the next one)
                checkAutoSave();

                // Next floor
                currentFloorNumber++;
                player.setFloorReached(currentFloorNumber);
                checkAchievements(); // Check for floor/level achievements
                logger.info("GAMELOOP", "Advancing to floor " + currentFloorNumber);
            } catch (Exception e) {
                logger.error("GAMELOOP", "Error in game loop at floor " + currentFloorNumber, e);
                view.showError("Error: " + e.getMessage());
                view.pressAnyKey();
                break;
            }
        }

        logger.info("GAMELOOP", "Game loop ended");
        gameOver();
    }

    // Poorly implemented achievement checking
    private void checkAchievements() {
        int kills = player.getMonstersDefeated();
        if (kills == 1 && achievementSystem.unlock("first_blood")) {
            view.showAchievement("First Blood", "Defeat your first monster");
        }
        if (kills >= 10 && achievementSystem.unlock("monster_slayer_10")) {
            view.showAchievement("Monster Slayer", "Defeat 10 monsters");
        }
        if (kills >= 50 && achievementSystem.unlock("monster_slayer_50")) {
            view.showAchievement("Monster Hunter", "Defeat 50 monsters");
        }
        if (kills >= 100 && achievementSystem.unlock("monster_slayer_100")) {
            view.showAchievement("Exterminator", "Defeat 100 monsters");
        }
        int floor = player.getFloorReached();
        if (floor >= 5 && achievementSystem.unlock("floor_5")) {
            view.showAchievement("Depth Diver", "Reach floor 5");
        }
        if (floor >= 10 && achievementSystem.unlock("floor_10")) {
            view.showAchievement("Deep Explorer", "Reach floor 10");
        }
        if (floor >= 20 && achievementSystem.unlock("floor_20")) {
            view.showAchievement("Master of the Maze", "Reach floor 20");
        }
        if (floor >= 50 && achievementSystem.unlock("floor_50")) {
            view.showAchievement("Legend of the Labyrinth", "Reach floor 50");
        }
        int level = player.getLevel();
        if (level >= 5 && achievementSystem.unlock("level_5")) {
            view.showAchievement("Seasoned Adventurer", "Reach level 5");
        }
        if (level >= 10 && achievementSystem.unlock("level_10")) {
            view.showAchievement("Veteran Explorer", "Reach level 10");
        }
        if (level >= 15 && achievementSystem.unlock("level_15")) {
            view.showAchievement("Hero of Legend", "Reach level 15");
        }
        if (level >= 20 && achievementSystem.unlock("level_20")) {
            view.showAchievement("Demigod", "Reach level 20");
        }
        int puzzles = player.getPuzzlesSolved();
        if (puzzles >= 5 && achievementSystem.unlock("puzzle_master")) {
            view.showAchievement("Puzzle Master", "Solve 5 puzzles");
        }
        if (puzzles >= 20 && achievementSystem.unlock("puzzle_genius")) {
            view.showAchievement("Enigma Solver", "Solve 20 puzzles");
        }
        int loreCount = player.getLoreCollection().size();
        if (loreCount >= 5 && achievementSystem.unlock("lore_collector")) {
            view.showAchievement("Lore Seeker", "Find 5 lore items");
        }
        if (loreCount >= 10 && achievementSystem.unlock("lore_complete")) {
            view.showAchievement("Chronicler", "Find all 10 lore items");
        }
        int mimics = player.getMimicsDefeated();
        if (mimics >= 3 && achievementSystem.unlock("mimic_hunter")) {
            view.showAchievement("Mimic Hunter", "Defeat 3 mimic chests");
        }
        if (mimics >= 10 && achievementSystem.unlock("mimic_nightmare")) {
            view.showAchievement("Mimic's Bane", "Defeat 10 mimic chests");
        }
        int crafted = player.getItemsCrafted();
        if (crafted >= 10 && achievementSystem.unlock("crafter")) {
            view.showAchievement("Master Crafter", "Craft 10 items");
        }
        int chests = player.getChestsOpened();
        if (chests >= 20 && achievementSystem.unlock("treasure_hunter")) {
            view.showAchievement("Treasure Hunter", "Open 20 treasure chests");
        }
        if (chests >= 100 && achievementSystem.unlock("treasure_hoarder")) {
            view.showAchievement("Dragon's Hoard", "Open 100 treasure chests");
        }
        long score = player.getTotalScore();
        if (score >= 10000 && achievementSystem.unlock("score_10k")) {
            view.showAchievement("High Roller", "Reach 10,000 points");
        }
        if (score >= 50000 && achievementSystem.unlock("score_50k")) {
            view.showAchievement("Score Master", "Reach 50,000 points");
        }
        if (score >= 100000 && achievementSystem.unlock("score_100k")) {
            view.showAchievement("Point Hoarder", "Reach 100,000 points");
        }
        if (score >= 500000 && achievementSystem.unlock("score_500k")) {
            view.showAchievement("Legendary Score", "Reach 500,000 points");
        }

    }

    private void exploreFloor() {
        logger.info("EXPLORE", "Starting floor exploration");
        try {
            // Warn player if approaching a boss floor (2-3 floors before)
            int floorsUntilBoss = 5 - (currentFloorNumber % 5);
            if (floorsUntilBoss >= 0 && floorsUntilBoss <= 3 && floorsUntilBoss != 5) {
                if (floorsUntilBoss == 0) {
                    // Currently on boss floor
                    view.printLine("");
                    view.printColoredLine("⚠⚠⚠ BOSS FLOOR WARNING ⚠⚠⚠", ConsoleColor.BRIGHT_RED);
                    view.printLine(ConsoleColor.BRIGHT_YELLOW + "You sense an overwhelming presence on this floor..."
                            + ConsoleColor.RESET);
                    view.printLine(ConsoleColor.BRIGHT_YELLOW
                            + "A powerful boss awaits. Make sure you are fully prepared!" + ConsoleColor.RESET);
                    view.printLine("");
                    view.pressAnyKey();
                } else if (floorsUntilBoss == 1) {
                    view.printLine("");
                    view.printColoredLine("⚠ Warning: Boss Approaching ⚠", ConsoleColor.BRIGHT_YELLOW);
                    view.printLine(ConsoleColor.YELLOW + "The air grows heavy. A boss lurks on the next floor."
                            + ConsoleColor.RESET);
                    view.printLine(ConsoleColor.YELLOW
                            + "Consider preparing: upgrade equipment, stock potions, level up." + ConsoleColor.RESET);
                    view.printLine("");
                    view.pressAnyKey();
                } else if (floorsUntilBoss == 2 || floorsUntilBoss == 3) {
                    view.printLine("");
                    view.printColoredLine("ℹ Hint: Boss Floor Ahead", ConsoleColor.BRIGHT_CYAN);
                    view.printLine(ConsoleColor.CYAN + "You'll face a boss in " + floorsUntilBoss
                            + " floors. Now's the time to prepare." + ConsoleColor.RESET);
                    view.printLine("");
                    view.pressAnyKey();
                }
            }

            // Initialize boss system for every 5th floor
            if (currentFloor.isBossFloor()) {
                currentFloor.initializeBossRoom();
                logger.info("EXPLORE", "Boss floor !");
            }

            view.showFloorHeader(currentFloor.getFloorNumber());
            view.printLine("You make it to Floor " + currentFloor.getFloorNumber());
            view.showMapLegend();

            boolean floorComplete = false;

            while (player.isAlive() && !floorComplete) {
                view.clear();
                view.showPlayerHUD(player);
                // Set player level and dungeon floor for dynamic monster coloring
                view.getMapView().setPlayerLevel(player.getLevel());
                view.getMapView().setDungeonFloor(currentFloor);
                view.getMapView().showDungeonMap(
                        currentFloor.getMap(),
                        currentFloor.getVisibleMask(),
                        currentFloor.getExploredMask(),
                        currentFloor.getPlayerX(),
                        currentFloor.getPlayerY(),
                        player.hasTorch());

                view.printLine("");
                if (!lastActionMessage.isEmpty()) {
                    view.printLine(lastActionMessage);
                    lastActionMessage = "";
                }
                view.printLine(
                        "[W/A/S/D] Move | [E] Interact | [I] Inventory | [1/3] Use Potion | [L] Lore | [C] Craft | [V] Save | [M] Map | [H] Help | [Q] Quit Game");
                view.print("Command > ");
                // In theory, this could be more modular

                try {
                    String command = view.readLine().trim();
                    logger.debug("EXPLORE", "Player command: " + command);

                    // Track player actions for boss roar system
                    currentFloor.incrementActionCounter();

                    // Check for debug console trigger
                    if (settings.debugMode && command.equalsIgnoreCase("DEBUG CONSOLE")) {
                        openDebugConsole();
                        continue;
                    }

                    // Check for slash commands
                    if (settings.debugMode && debugSystem != null && command.startsWith("/")) {
                        debugSystem.executeCommand(command);
                        view.pressAnyKey();
                        continue;
                    }

                    String upperCommand = command.toUpperCase();
                    switch (upperCommand) {
                        case "W" -> handleMove(0, -1);
                        case "A" -> handleMove(-1, 0);
                        case "S" -> handleMove(0, 1);
                        case "D" -> handleMove(1, 0);
                        case "E" -> handleInteract();
                        case "I" -> handleInventory();
                        case "1" -> useQuickPotion(ItemType.POTION_HEALTH);
                        // case "2": Mana disabled
                        // useQuickPotion(ItemType.POTION_MANA);
                        case "3" -> useQuickPotion(ItemType.POTION_STAMINA);
                        case "L" -> handleLoreCollection();
                        case "C" -> handleCrafting();
                        case "V" -> handleSaveGame();
                        case "M" -> handleMap();
                        case "H" -> handleHelp();
                        case "Q" -> {
                            // Quit game from SAFE rooms only; otherwise offer options
                            Room curRoom = currentFloor.getRoomAt(currentFloor.getPlayerX(), currentFloor.getPlayerY());
                            if (curRoom != null && curRoom.getType() == Room.RoomType.SAFE) {
                                view.printLine("");
                                view.printLine(ConsoleColor.BRIGHT_YELLOW + "QUIT GAME" + ConsoleColor.RESET);
                                view.printLine("  [S] Save and Quit to Main Menu");
                                view.printLine("  [Q] Quit without Saving");
                                view.printLine("  [B] Back to Game");
                                view.print("> ");
                                String qChoice = view.readLine().trim();
                                if (qChoice.equalsIgnoreCase("s")) {
                                    handleSaveGame();
                                    floorComplete = true;
                                    running = false;
                                    lastActionMessage = ConsoleColor.BRIGHT_GREEN
                                            + "✓ Game saved and returned to main menu" + ConsoleColor.RESET;
                                } else if (qChoice.equalsIgnoreCase("q")) {
                                    floorComplete = true;
                                    running = false;
                                    lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "✓ Quit to main menu (not saved)"
                                            + ConsoleColor.RESET;
                                }
                            } else {
                                view.printLine("");
                                view.showWarning("Quit is only allowed in Safe Rooms.");
                                view.printLine(
                                        "Options: [P] Pay " + SAVE_QUIT_SCORE_COST
                                                + " score to quit now | [H] Highlight nearest Safe Room | [B] Back");
                                view.print("> ");
                                String qChoice = view.readLine().trim();
                                if (qChoice.equalsIgnoreCase("p")) {
                                    if (spendScore(SAVE_QUIT_SCORE_COST)) {
                                        view.printLine("  [S] Save and Quit to Main Menu");
                                        view.printLine("  [Q] Quit without Saving");
                                        view.printLine("  [B] Back to Game");
                                        view.print("> ");
                                        String finalChoice = view.readLine().trim();
                                        if (finalChoice.equalsIgnoreCase("s")) {
                                            handleSaveGame(true); // true = cost already paid
                                            floorComplete = true;
                                            running = false;
                                            lastActionMessage = ConsoleColor.BRIGHT_GREEN
                                                    + "✓ Game saved (" + SAVE_QUIT_SCORE_COST + " score spent)"
                                                    + ConsoleColor.RESET;
                                        } else if (finalChoice.equalsIgnoreCase("q")) {
                                            floorComplete = true;
                                            running = false;
                                            lastActionMessage = ConsoleColor.BRIGHT_YELLOW
                                                    + "✓ Quit to main menu (" + SAVE_QUIT_SCORE_COST
                                                    + " score spent, not saved)"
                                                    + ConsoleColor.RESET;
                                        }
                                    } else {
                                        view.showWarning("Not enough score (" + SAVE_QUIT_SCORE_COST + " required).");
                                        view.pressAnyKey();
                                    }
                                } else if (qChoice.equalsIgnoreCase("h")) {
                                    highlightNearestSafeRoomOnMap();
                                }
                            }
                        }
                        default -> {
                            if (settings.debugMode) {
                                lastActionMessage = ConsoleColor.BRIGHT_YELLOW
                                        + "⚠ Unknown command. Type 'DEBUG CONSOLE' for debug menu" + ConsoleColor.RESET;
                            } else {
                                lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "⚠ Unknown command"
                                        + ConsoleColor.RESET;
                            }
                        }
                    }

                    // Check if reached end
                    if (currentFloor.hasReachedEnd()) {
                        // Check if this floor has an exit room
                        if (currentFloor.hasExitRoom()) {
                            handleExitEncounter();
                            // Mark exit room as completed regardless of choice
                            Room exitRoom = currentFloor.getExitRoom();
                            if (exitRoom != null) {
                                exitRoom.setCompleted(true);
                            }
                            // If player chose to leave, break floor loop
                            if (!running) {
                                lastActionMessage = ""; // Clear message for game over screen
                                floorComplete = true;
                            }
                        } else {
                            lastActionMessage = ConsoleColor.BRIGHT_GREEN + "✓ You reached the end of the floor!"
                                    + ConsoleColor.RESET;
                            floorComplete = true;
                        }
                    }

                } catch (Exception e) {
                    logger.error("EXPLORE", "Input error during exploration", e);
                    view.showError("Input error: " + e.getMessage());
                    // Prevent potential infinite loop if input stream is invalid/closed
                    view.showInfo("Returning to previous menu due to input error.");
                    break;
                }
            }
            logger.info("EXPLORE", "Floor exploration complete");
        } catch (Exception e) {
            logger.error("EXPLORE", "Fatal error during floor exploration", e);
            view.showError("Fatal error during exploration: " + e.getMessage());
            view.pressAnyKey();
        }
    }

    private void handleMove(int dx, int dy) {
        // Handle player movement and check for hazards/encounters
        // dx, dy: direction vector (-1, 0, 1) representing up/down/left/right

        int newX = currentFloor.getPlayerX() + dx;
        int newY = currentFloor.getPlayerY() + dy;

        // Update player position
        currentFloor.setPlayerPosition(newX, newY);

        // Players slowly regenerate stamina while exploring (passive recovery between
        // fights)
        regenerateStamina();

        // Check for traps at the new position (tile is '^')
        // Trap damage scales with floor progression - creates escalating danger
        // Formula: TRAP_BASE_DAMAGE + (random 0-9 * current_floor_number)
        // Examples:
        // Floor 1: 5 + (0-9 * 1) = 5-14 damage (manageable)
        // Floor 10: 5 + (0-9 * 10) = 5-95 damage (lethal)
        // This means later floors are EXPONENTIALLY more dangerous from traps
        // TODO: Consider adding trap resistance via armor or skills to avoid one-shots
        char tile = currentFloor.getTileAt(newX, newY);
        if (tile == '^') {
            int floorScaling = rng.nextInt(10) * currentFloor.getFloorNumber();
            int trapDamage = TRAP_BASE_DAMAGE + floorScaling;
            player.takeDamage(trapDamage);
            lastActionMessage = ConsoleColor.BRIGHT_RED + "⚠ You triggered a trap! Took " + trapDamage + " damage!"
                    + ConsoleColor.RESET;
            logger.warn("TRAP", "Player triggered trap for " + trapDamage + " damage");
        }

        // After player moves, all monsters on the floor move toward the player "sound"
        // todo: Make this smarter / more interesting, less linear
        // todo: In future, consider shared monster "AI" / "knowledge" of player
        // so when a monster dies, the others know it - if a boss dies however, monsters
        // flee / go away from player
        // Perhaps another monster could "become" the boss ?
        currentFloor.moveMonsters();

        // Check if current floor has a boss that should roar/reveal
        if (currentFloor.isBossFloor()) {
            checkBossRoar();
        }

        // Check if player entered a special room (treasure, puzzle, or combat)
        Room room = currentFloor.getRoomAt(newX, newY);
        if (room != null && !room.isCompleted()) {
            if (room.getType() == Room.RoomType.TREASURE) {
                // Treasury: open treasure chests and collect loot
                handleTreasure(room);
            } else if (room.getType() == Room.RoomType.PUZZLE && settings.enablePuzzles) {
                // Puzzle: solve a puzzle (tic-tac-toe or riddle)
                handlePuzzle(room);
            } else if (room.getType() == Room.RoomType.BOSS) {
                // Boss encounter: fight the boss
                if (!room.isExplored() && room.getMonsters().size() > 0) {
                    logger.debug("BOSS", "Triggering boss encounter");
                    handleEncounter(room);
                } else {
                    logger.debug("BOSS", "Skipping boss encounter - explored=" + room.isExplored() +
                            ", monsters=" + room.getMonsters().size());
                }
            } else if (room.getType() == Room.RoomType.ENCOUNTER) {
                // Combat: fight monsters in encounter room
                if (!room.isExplored() && room.getMonsters().size() > 0) {
                    logger.debug("COMBAT", "Triggering encounter: explored=" + room.isExplored() +
                            ", monsters=" + room.getMonsters().size() + ", elite=" + room.isElite());
                    handleEncounter(room);
                } else {
                    logger.debug("COMBAT", "Skipping encounter - explored=" + room.isExplored() +
                            ", monsters=" + room.getMonsters().size() + ", elite=" + room.isElite());
                }
            }
        }
    }

    private void handleEncounter(Room room) {
        logger.info("COMBAT", "Starting encounter with " + room.getMonsters().size() + " enemies");
        try {
            // Show encounter warning with number of enemies
            int enemyCount = room.getMonsters().size();
            String enemyText = enemyCount == 1 ? "enemy" : "enemies";
            view.printLine("");
            if (room.getType() == Room.RoomType.BOSS) {
                view.showWarning("⚠ A powerful BOSS emerges! ⚠");
            } else if (room.isElite()) {
                view.showWarning("⚠ " + enemyCount + " ELITE " + enemyText + " ahead! ⚠");
            } else {
                view.printLine(ConsoleColor.BRIGHT_YELLOW + "You have encountered " + enemyCount + " " + enemyText + "!"
                        + ConsoleColor.RESET);
            }
            view.pressAnyKey();

            for (int i = 0; i < room.getMonsters().size(); i++) {
                Monster monster = room.getMonsters().get(i);
                checkLowHealthWarning(); // Safety: Warn before combat
                CombatSystem combat = new CombatSystem(player, monster, view);
                combat.start();

                // Check for level up and stat allocation
                checkAndDisplayLevelUp(combat);

                // Increment monster defeat counter if player won
                if (player.isAlive() && !monster.isAlive()) {
                    player.incrementMonstersDefeated();
                }

                // LOOT DROP SYSTEM - Regular monsters drop items based on multiple factors
                if (player.isAlive() && !monster.isAlive() && !room.isElite() && room.getType() != Room.RoomType.BOSS) {
                    dropNormalMonsterLoot(monster);
                }

                // Check achievements after combat
                if (player.isAlive()) {
                    checkAchievements();
                }

                if (!player.isAlive()) {
                    break;
                }
            }

            if (player.isAlive()) {
                room.setExplored(true);
                room.setCompleted(true);
                // Remove monster symbols from map after clearing room
                currentFloor.clearRoomMonsters(room);

                // Boss room special loot
                if (room.getType() == Room.RoomType.BOSS && currentFloor.isBossFloor()) {
                    // Drop boss key (required to escape boss room)
                    Item bossKey = ItemFactory.createBossKey();
                    if (canAddToInventory()) {
                        player.addItem(bossKey);
                        view.showSuccess("BOSS DEFEATED! " + bossKey.getName() + " obtained!");
                    }

                    // Drop legendary gear
                    Item bossLoot = ItemFactory.generateBossLoot(rng, currentFloor.getFloorNumber());
                    if (canAddToInventory()) {
                        player.addItem(bossLoot);
                        view.showSuccess("Legendary loot acquired: " + bossLoot.getName());
                    }
                    logger.info("COMBAT", "Boss defeated - boss key and legendary loot dropped");
                } else if (room.isElite()) {
                    Rarity eliteRarity = Rarity.RARE;
                    Item bonus = rng.nextInt(2) == 0 ? ItemFactory.randomWeapon(rng, eliteRarity)
                            : ItemFactory.randomArmor(rng, eliteRarity);
                    if (canAddToInventory()) {
                        player.addItem(bonus);
                        view.showSuccess("Elite loot: " + bonus.getName() + " (" + eliteRarity + ")");
                    }
                }
                view.showSuccess("Room cleared!");
                logger.info("COMBAT", "Room cleared successfully");
            } else {
                logger.warn("COMBAT", "Player died in combat");
            }
        } catch (Exception e) {
            logger.error("COMBAT", "Error during encounter", e);
            view.showError("Combat error: " + e.getMessage());
            view.pressAnyKey();
        }
    }

    /**
     * Drop loot from normal (non-elite, non-boss) monsters
     * Loot quality and quantity scale with:
     * - Player level (higher level = better loot)
     * - Monster level (higher level monsters = better drops)
     * - Monster rarity (rarer monsters = better loot)
     * - Current floor (deeper floors = better rewards)
     * - Difficulty setting
     */
    private void dropNormalMonsterLoot(Monster monster) {
        logger.debug("LOOT", "Rolling loot for " + monster.getName() + " (Lvl " + monster.getLevel() + ", "
                + monster.getRarity() + ")");

        // Base drop chance: 40% for items, 30% for potions
        // Monster rarity increases drop chances significantly
        double rarityDropBonus = monster.getRarity().tier * 0.15; // +15% per rarity tier
        double itemDropChance = 0.40 + rarityDropBonus;
        double potionDropChance = 0.30 + (rarityDropBonus * 0.5); // Potions less affected by rarity

        // Player luck affects drop rates (1% per luck point)
        itemDropChance += player.getLuck() * 0.01;
        potionDropChance += player.getLuck() * 0.01;

        // Cap drop chances at 95%
        itemDropChance = Math.min(0.95, itemDropChance);
        potionDropChance = Math.min(0.95, potionDropChance);

        boolean droppedSomething = false;

        // Roll for item drop (weapon or armor)
        if (rng.nextDouble() < itemDropChance) {
            // Determine rarity based on monster level, player level, floor, and difficulty
            Rarity lootRarity = ItemFactory.rollRarity(rng, settings.difficulty, currentFloorNumber, player.getLevel());

            // Higher rarity monsters can't drop items worse than one tier below them
            // Example: RARE monster won't drop COMMON items
            if (monster.getRarity().tier > 0) {
                int minTier = Math.max(0, monster.getRarity().tier - 1);
                if (lootRarity.tier < minTier) {
                    lootRarity = Rarity.values()[minTier];
                }
            }

            // 50/50 weapon or armor
            Item loot = rng.nextInt(2) == 0
                    ? ItemFactory.randomWeapon(rng, lootRarity)
                    : ItemFactory.randomArmor(rng, lootRarity);

            if (canAddToInventory()) {
                player.addItem(loot);
                view.printLine(
                        ConsoleColor.BRIGHT_YELLOW + "⚔ Loot: " + loot.getName() + " (" + lootRarity.getColoredName()
                                + ")" + ConsoleColor.RESET);
                logger.info("LOOT", "Dropped: " + loot.getName() + " (" + lootRarity + ")");
            }
            droppedSomething = true;
        }

        // Roll for potion drop
        if (rng.nextDouble() < potionDropChance) {
            Item potion = ItemFactory.randomPotion(rng);
            player.addItem(potion);
            view.printLine(ConsoleColor.BRIGHT_CYAN + "⚗ Loot: " + potion.getName() + ConsoleColor.RESET);
            logger.info("LOOT", "Dropped: " + potion.getName());
            droppedSomething = true;
        }

        // Small chance for bonus loot from higher rarity monsters
        if (monster.getRarity().tier >= Rarity.RARE.tier) {
            double bonusChance = 0.15 * monster.getRarity().tier; // 15% per tier above common
            if (rng.nextDouble() < bonusChance) {
                Item bonusPotion = ItemFactory.randomPotion(rng);
                player.addItem(bonusPotion);
                view.printLine(ConsoleColor.BRIGHT_GREEN + "✨ Bonus: " + bonusPotion.getName() + ConsoleColor.RESET);
                logger.info("LOOT", "Bonus drop: " + bonusPotion.getName());
                droppedSomething = true;
            }
        }

        if (!droppedSomething) {
            logger.debug("LOOT", "No drops from " + monster.getName());
        }
    }

    private void handlePuzzle(Room room) {
        logger.info("PUZZLE", "Starting puzzle in room");
        try {
            boolean puzzleCompleted = false;

            switch (room.getPuzzleType()) {
                case TIC_TAC_TOE -> {
                    TicTacToePuzzle ticTacToe = new TicTacToePuzzle(view, rng);
                    puzzleCompleted = ticTacToe.play();
                }
                case SPHINX_RIDDLE -> {
                    SphinxPuzzle sphinx = new SphinxPuzzle(view, rng);
                    puzzleCompleted = sphinx.play();
                }
                default -> {
                    view.showWarning("Unknown puzzle type");
                    logger.warn("PUZZLE", "Unknown puzzle type: " + room.getPuzzleType());
                    return;
                }
            }

            if (puzzleCompleted) {
                room.setCompleted(true);
                room.setExplored(true);

                // Grant reward for solving puzzle
                int xpReward = 100 + (currentFloor.getFloorNumber() * 20);
                int scoreReward = 500;
                boolean leveledUp = player.gainExperience(xpReward);
                player.addScore(scoreReward);

                view.showSuccess("Puzzle solved! Gained " + xpReward + " XP and " + scoreReward + " points!");

                // Check for stat allocation if leveled up
                if (leveledUp) {
                    view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
                    view.showStatAllocation(player);
                }

                logger.info("PUZZLE", "Puzzle solved successfully, awarded " + xpReward + " XP");
                view.pressAnyKey();
            } else {
                view.showWarning("Puzzle failed! You must fight your way through!");
                logger.warn("PUZZLE", "Player failed puzzle, spawning penalty monster");

                // Puzzle failure spawns a special penalty encounter
                // Puzzle Guardian is slightly stronger than normal floor encounters
                // Level = floor + 2 creates meaningful challenge without being unfair
                // Example: Floor 5 puzzle failure = level 7 guardian (normally max level 5)
                // Rarity = RARE to make victory feel earned, not impossible
                // Dark type with Light weakness ensures elemental matchups matter

                int penaltyLevel = currentFloor.getFloorNumber() + 2;
                Monster penalty = MonsterFactory.createPuzzleGuardian(penaltyLevel);
                checkLowHealthWarning(); // Safety: Warn before combat
                CombatSystem combat = new CombatSystem(player, penalty, view);
                combat.start();

                // Check for level up and stat allocation
                if (combat.didPlayerLevelUp()) {
                    view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
                    view.showStatAllocation(player);
                }

                if (player.isAlive()) {
                    player.incrementMonstersDefeated();
                    room.setCompleted(true);
                    room.setExplored(true);
                }
            }
        } catch (Exception e) {
            // How did Paul reach this...
            // This whole system should work better
            logger.error("PUZZLE", "Error during puzzle", e);
            view.showError("Puzzle error: " + e.getMessage());
            view.pressAnyKey();
        }
    }

    private void handleTreasure(Room room) {
        logger.info("TREASURE", "Player entered treasure room");

        // Increment loot counter at START so it reflects the number of times looted
        room.incrementRelootCount();
        int relootCount = room.getRelootCount();

        // Check if this is the starting room (first room in rooms list) - they don't
        // have re-loot penalties
        boolean isStartingRoom = (currentFloor.getRooms().size() > 0 &&
                currentFloor.getRooms().get(0) == room);

        // Check for re-loot penalty: monsters appear if room has been looted multiple
        // times
        // (starting room is exempt from this mechanic)
        if (!isStartingRoom && relootCount > 1) { // Only on 2nd+ loots
            int encounterChance = getRelootEncounterChance(relootCount);
            logger.info("TREASURE", "Re-loot check: loot #" + relootCount + ", chance=" + encounterChance + "%");

            if (encounterChance > 0 && rng.nextInt(100) < encounterChance) {
                // Re-loot encounter triggered!
                view.showWarning("\n⚠ The room suddenly grows colder...");
                view.showWarning("Supernatural guardians emerge from the shadows!");
                logger.warn("TREASURE", "Re-loot encounter TRIGGERED at count=" + relootCount + ", chance was "
                        + encounterChance + "%");

                // Create guardian monster scaled to current floor
                Monster guardian = MonsterFactory.createTreasureGuardian(currentFloor.getFloorNumber() + 1);

                checkLowHealthWarning(); // Safety: Warn before combat
                CombatSystem combat = new CombatSystem(player, guardian, view);
                combat.start();

                // Check for level up and stat allocation
                if (combat.didPlayerLevelUp()) {
                    view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
                    view.showStatAllocation(player);
                }

                if (player.isAlive()) {
                    player.incrementMonstersDefeated();
                    view.showSuccess("You defeated the guardian!");
                    room.setExplored(true);
                    logger.info("TREASURE", "Guardian defeated, continuing with loot");
                    // Continue to loot after defeating guardian
                } else {
                    logger.warn("TREASURE", "Player died to re-loot guardian");
                    return; // Exit without giving loot
                }
            }
        }

        // Normal treasure looting logic

        // Check if it's a mimic
        if (room.isMimic()) {
            view.showWarning("The chest suddenly springs to life!");
            view.printLine("It's a MIMIC!");
            logger.warn("TREASURE", "Mimic chest triggered");

            // Create mimic using MonsterFactory
            Monster mimic = MonsterFactory.createMimic(currentFloor.getFloorNumber() + 1);

            checkLowHealthWarning(); // Safety: Warn before combat
            CombatSystem combat = new CombatSystem(player, mimic, view);
            combat.start();

            // Check for level up and stat allocation
            checkAndDisplayLevelUp(combat);

            if (player.isAlive()) {
                player.incrementMonstersDefeated();
                view.showSuccess("You defeated the mimic!");
                room.setCompleted(true);
                room.setExplored(true);
                logger.info("TREASURE", "Mimic defeated");
            }
        } else {
            view.printLine("\nYou found a treasure chest!");

            // Show re-loot warning if applicable
            if (relootCount > 1) {
                view.showWarning("(⚠ This is your " + relootCount + "th time looting this room)");
            }

            // Check for lore item
            if (room.getLoreItem() != null) {
                view.showSuccess("═══════════════════════════════════════════════════════");
                view.showSuccess("  LORE DISCOVERED!");
                view.showSuccess("═══════════════════════════════════════════════════════");
                view.printLine("");
                view.printLine("You found: " + room.getLoreItem().getName());
                view.printLine(room.getLoreItem().getDescription());
                view.printLine("");
                view.printLine("--- LORE TEXT ---");
                view.printLine(room.getLoreItem().getLoreText());
                view.printLine("");

                player.addLoreItem(room.getLoreItem());
                player.addScore(LORE_ITEM_SCORE_REWARD);
                logger.info("TREASURE", "Lore item collected: " + room.getLoreItem().getName());
            }

            // Regular treasure
            int goldFound = TREASURE_GOLD_BASE
                    + (rng.nextInt(TREASURE_GOLD_MULTIPLIER) * currentFloor.getFloorNumber());
            player.addScore(goldFound);
            view.printLine("You found " + goldFound + " gold!");

            // Chance to find a map fragment revealing nearby rooms
            if (rng.nextInt(100) < MAP_FRAGMENT_CHANCE) {
                Item mapFrag = new Item("Map Fragment", ItemType.MAP_FRAGMENT, ElementType.NEUTRAL, Rarity.COMMON);
                player.addItem(mapFrag);
                currentFloor.revealRandomRooms(MAP_FRAGMENT_REVEAL_ROOMS);
                view.showSuccess("You piece together a map fragment and reveal parts of the floor!");
            }

            // Grant gear drops - starting room gets better guaranteed loot
            if (isStartingRoom) {
                // Starting room: guarantee UNCOMMON or better gear to help player
                view.showSuccess("═══ STARTING CACHE ═══");
                view.printLine("This chest contains supplies left by previous adventurers!");

                // Give both a weapon AND armor
                Rarity startingRarity = settings.difficulty == Difficulty.TUTORIAL ? Rarity.UNCOMMON
                        : settings.difficulty == Difficulty.EASY ? Rarity.UNCOMMON : Rarity.COMMON;

                Item weapon = ItemFactory.randomWeapon(rng, startingRarity);
                Item armor = ItemFactory.randomArmor(rng, startingRarity);

                player.addItem(weapon);
                player.addItem(armor);

                // This section is a little complicated, so here's an explanation
                // The system checks the difficulty setting to determine the minimum rarity of
                // the starting gear
                // On TUTORIAL and EASY, the player is guaranteed UNCOMMON gear
                // On NORMAL and HARD, the player gets COMMON gear, which may be upgraded via
                // RNG
                // This helps new players get a better start while keeping challenge for
                // "better" players

                view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ You found: " + weapon.getName() + " ("
                        + startingRarity.getColoredName() + ConsoleColor.BRIGHT_GREEN + ")" + ConsoleColor.RESET);
                view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ You found: " + armor.getName() + " ("
                        + startingRarity.getColoredName() + ConsoleColor.BRIGHT_GREEN + ")" + ConsoleColor.RESET);

                // Also give a healing potion
                Item potion = ItemFactory.randomPotion(rng);
                player.addItem(potion);
                view.printLine("You found: " + potion.getName());

                logger.info("TREASURE", "Starting room treasure: weapon, armor, and potion");
            } else {
                // Regular treasure room
                Rarity dropRarity = ItemFactory.rollRarity(rng, settings.difficulty, currentFloor.getFloorNumber(),
                        player.getLevel());
                boolean dropArmor = rng.nextInt(2) == 0;
                Item loot = dropArmor ? ItemFactory.randomArmor(rng, dropRarity)
                        : ItemFactory.randomWeapon(rng, dropRarity);
                if (canAddToInventory()) {
                    player.addItem(loot);
                    view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ You found: " + loot.getName() + " ("
                            + dropRarity.getColoredName() + ConsoleColor.BRIGHT_GREEN + ")" + ConsoleColor.RESET);
                }
            }

            room.setCompleted(true);
            room.setExplored(true);
            logger.info("TREASURE", "Treasure collected: " + goldFound + " gold");
            view.pressAnyKey();
        }
    }

    /**
     * Calculates the chance (0-100) that a re-loot encounter will trigger.
     * Scales progressively with the number of times a room has been looted:
     * - 1 loot: 0% (first loot is always safe)
     * - 5 loots: 1%
     * - 10 loots: 10%
     * - 15 loots: 25%
     * - 20 loots: 50%
     * - 25 loots: 75%
     * - 30+ loots: 100%
     */
    private int getRelootEncounterChance(int relootCount) {
        if (relootCount < 5)
            return 0;
        if (relootCount < 10)
            return 1;
        if (relootCount < 15)
            return 10;
        if (relootCount < 20)
            return 25;
        if (relootCount < 25)
            return 50;
        if (relootCount < 30)
            return 75;
        return 100;
    }

    private void regenerateStamina() {
        // Regenerate stamina after actions like moving
        int regenAmount = (int) (player.getMaxStamina() * STAMINA_REGEN_RATE);
        int oldStamina = player.getStamina();
        player.restoreStamina(regenAmount);
        int newStamina = player.getStamina();

        if (newStamina > oldStamina) {
            logger.debug("STAMINA", "Regenerated " + (newStamina - oldStamina) + " stamina (" + newStamina + "/"
                    + player.getMaxStamina() + ")");
        }

        // PASSIVE HEALTH REGENERATION: 2-5% of max HP per action (based on resilience)
        // Higher resilience = faster natural healing
        double healthRegenRate = 0.02 + (player.getResilience() / 1000.0); // 2-5% based on resilience
        int healthRegen = (int) (player.getMaxHealth() * healthRegenRate);
        if (healthRegen > 0 && player.getHealth() < player.getMaxHealth()) {
            int oldHealth = player.getHealth();
            player.heal(healthRegen);
            int healed = player.getHealth() - oldHealth;
            if (healed > 0) {
                logger.debug("HEALTH", "Regenerated " + healed + " health (" + player.getHealth() + "/"
                        + player.getMaxHealth() + ")");
            }
        }

        // Tick potion HoT effects during exploration
        String hotMessage = player.tickPotionHoT();
        if (!hotMessage.isEmpty() && !lastActionMessage.isEmpty()) {
            lastActionMessage += "\n" + hotMessage;
        } else if (!hotMessage.isEmpty()) {
            lastActionMessage = hotMessage;
        }
    }

    private void useQuickPotion(ItemType potionType) {
        // Find first potion of the specified type
        Item potion = null;
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getType() == potionType) {
                potion = item;
                break;
            }
        }

        if (potion == null) {
            String potionName = getPotionTypeName(potionType);
            lastActionMessage = ConsoleColor.BRIGHT_RED + "✖ No " + potionName + " Potions!" + ConsoleColor.RESET;
            return;
        }

        // NEW POTION SYSTEM: Calculate HoT values based on player stats
        int[] hotValues = potion.getPotionHoTValues(player.getIntelligence(), player.getLevel(), player.getLuck());
        int initialHeal = hotValues[0];
        int hotTotal = hotValues[1];
        int hotTurns = hotValues[2];

        String potionTypeName = getPotionTypeName(potionType);

        // Apply potion HoT effect
        switch (potionType) {
            case POTION_HEALTH -> {
                player.applyPotionHoT(initialHeal, hotTotal, hotTurns, true);
                lastActionMessage = ConsoleColor.BRIGHT_GREEN + "✓ Used " + potionTypeName + " Potion! (+"
                        + initialHeal + " HP now, +" + hotTotal + " HP over " + hotTurns + " turns)"
                        + ConsoleColor.RESET;
            }
            case POTION_STAMINA -> {
                player.applyPotionHoT(initialHeal, hotTotal, hotTurns, false);
                lastActionMessage = ConsoleColor.BRIGHT_CYAN + "✓ Used " + potionTypeName + " Potion! (+"
                        + initialHeal + " ST now, +" + hotTotal + " ST over " + hotTurns + " turns)"
                        + ConsoleColor.RESET;
            }
            default -> {
                logger.warn("POTION", "Unknown potion type: " + potionType);
                return;
            }
        }

        player.removeItem(potion);
        logger.info("POTION", "Used " + potionTypeName + " potion: +" + initialHeal + " now, +" + hotTotal + " over "
                + hotTurns + " turns");

        // Tick potion HoT immediately after use to show effect
        String hotMessage = player.tickPotionHoT();
        if (!hotMessage.isEmpty()) {
            lastActionMessage += "\n" + hotMessage;
        }
    }

    private void handleQuickUsePotion(Item potion) {
        if (potion == null || !potion.getType().isPotion()) {
            view.showWarning("⚠ Not a potion!");
            return;
        }

        // NEW POTION SYSTEM: Calculate HoT values based on player stats
        int[] hotValues = potion.getPotionHoTValues(player.getIntelligence(), player.getLevel(), player.getLuck());
        int initialHeal = hotValues[0];
        int hotTotal = hotValues[1];
        int hotTurns = hotValues[2];

        String potionTypeName = switch (potion.getType()) {
            case POTION_HEALTH -> {
                player.applyPotionHoT(initialHeal, hotTotal, hotTurns, true);
                yield "Health";
            }
            // Mana disabled
            case POTION_STAMINA -> {
                player.applyPotionHoT(initialHeal, hotTotal, hotTurns, false);
                yield "Stamina";
            }
            default -> "Unknown";
        };

        view.showSuccess("✓ Used " + potionTypeName + " Potion! (+" + initialHeal + " now, +"
                + hotTotal + " over " + hotTurns + " turns)");
        logger.info("POTION", "Used " + potionTypeName + " potion: +" + initialHeal + " now, +"
                + hotTotal + " over " + hotTurns + " turns");
    }

    private void handleExitEncounter() {
        view.clear();
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "═══════════════════════════════════════" + ConsoleColor.RESET);
        view.printCentered(ConsoleColor.BRIGHT_YELLOW + "    ✦ GATEWAY TO FREEDOM AWAITS ✦" + ConsoleColor.RESET);
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "═══════════════════════════════════════" + ConsoleColor.RESET);
        view.printLine("");
        view.printLine("You stand before a shimmering portal, pulsing with otherworldly energy.");
        view.printLine("It calls to you with the promise of escape from this endless dungeon.");
        view.printLine("");
        view.printLine("The labyrinth stretches deeper still, full of treasures yet to claim,");
        view.printLine("monsters yet to defeat, and secrets yet to uncover...");
        view.printLine("");
        view.printLine("Current Floor: " + currentFloorNumber);
        view.printLine("Current Level: " + player.getLevel());
        view.printLine("Total Score: " + player.getTotalScore());
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_CYAN + "  [L] Leave the Dungeon (End Run)" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_YELLOW + "  [C] Continue Deeper" + ConsoleColor.RESET);
        view.print("> ");

        try {
            String choice = view.readLine().trim().toUpperCase();

            if (choice.equals("L")) {
                // Player chose to leave - end the game successfully
                player.setChosenExit(true);
                lastActionMessage = ConsoleColor.BRIGHT_GREEN
                        + "✓ You escaped the dungeon and reach safety!" + ConsoleColor.RESET;
                view.printLine(lastActionMessage);
                view.pressAnyKey();
                running = false; // This will trigger gameOver after the floor loop
            } else if (choice.equals("C")) {
                // Player chose to continue - just proceed normally
                lastActionMessage = ConsoleColor.BRIGHT_YELLOW
                        + "✓ You venture deeper into the dungeon. The exit will appear again in 5 floors."
                        + ConsoleColor.RESET;
                // Don't set floorComplete - let normal flow continue
            } else {
                lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "⚠ Invalid choice" + ConsoleColor.RESET;
                // If invalid, default to continuing
            }
        } catch (Exception e) {
            logger.error("EXIT", "Error during exit encounter", e);
            view.showError("Error: " + e.getMessage());
            lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "You remain in the dungeon." + ConsoleColor.RESET;
        }
    }

    private void handleInteract() {
        Room room = currentFloor.getRoomAt(currentFloor.getPlayerX(), currentFloor.getPlayerY());
        if (room != null && room.getType() == Room.RoomType.SAFE && !room.isCompleted()) {
            handleTemple(room);
            return;
        }

        lastActionMessage = ConsoleColor.BRIGHT_CYAN + "ℹ Looking around..." + ConsoleColor.RESET;

        // Interactions!
        int roll = rng.nextInt(100);
        if (roll < INTERACT_POTION_CHANCE) {
            // Find a potion
            Item found = ItemFactory.randomPotion(rng);
            player.addItem(found);
            lastActionMessage = ConsoleColor.BRIGHT_GREEN + "✓ You found a " + found.getName() + "!"
                    + ConsoleColor.RESET;
            logger.info("INTERACT", "Player found potion: " + found.getName());
        } else if (roll < INTERACT_GOLD_CHANCE) {
            // Find gold
            int goldFound = INTERACT_GOLD_MIN + rng.nextInt(50 * currentFloor.getFloorNumber());
            player.addScore(goldFound);
            lastActionMessage = ConsoleColor.BRIGHT_YELLOW + "✓ You found " + goldFound + " gold!"
                    + ConsoleColor.RESET;
            logger.info("INTERACT", "Player found " + goldFound + " gold");
        } else if (roll < 60) {
            // 10% chance to reveal nearby rooms
            currentFloor.revealRandomRooms(2);
            lastActionMessage = ConsoleColor.BRIGHT_CYAN
                    + "✓ You discovered a hidden map fragment! Nearby rooms revealed."
                    + ConsoleColor.RESET;
            logger.info("INTERACT", "Player revealed nearby rooms");
        } else {
            // 40% chance to find nothing
            lastActionMessage = ConsoleColor.BRIGHT_BLACK + "Nothing of interest here..." + ConsoleColor.RESET;
        }
    }

    private void handleTemple(Room room) { // Temple or shrine room! Originally idea was to hire Gods are party members
                                           // to slay Daedalus. Didn't happen.
        view.clear();
        view.printCentered("─── TEMPLE ───");
        view.printLine("Sacrifice score for a divine boon.");
        int floorCostBase = TEMPLE_COST_BASE + (currentFloor.getFloorNumber() * TEMPLE_COST_PER_FLOOR);
        int levelCost = floorCostBase;
        int gearCost = floorCostBase - TEMPLE_GEAR_COST_OFFSET;
        view.printLine("[1] Offer " + levelCost + " score for a level blessing");
        view.printLine("[2] Offer " + gearCost + " score to bless your weapon");
        view.printLine("[3] Offer " + gearCost + " score to bless your armor");
        view.printLine("[4] Leave");
        view.printLine("");
        view.print("Choice > ");

        String choice = view.readLine().trim();
        switch (choice) {
            case "1" -> {
                if (!spendScore(levelCost)) {
                    view.showWarning("Not enough score.");
                    return;
                }
                int xp = player.getExperienceToNextLevel();
                // 70% chance to get +1 level, 30% chance +2 levels
                boolean leveledUp = player.gainExperience(xp + (rng.nextInt(100) < 30 ? xp : 0));
                view.showSuccess("The gods grant you strength! You feel more experienced.");

                // Check for stat allocation if leveled up
                if (leveledUp) {
                    view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
                    view.showStatAllocation(player);
                }
            }
            case "2" -> {
                if (!spendScore(gearCost)) {
                    view.showWarning("Not enough score.");
                    return;
                }
                Item weapon = player.getEquippedWeapon();
                if (weapon == null) {
                    view.showWarning("You have no weapon equipped.");
                    return;
                }
                Item blessed = blessItem(weapon, true);
                player.addItem(blessed);
                player.equipWeapon(blessed);
                view.showSuccess("Your weapon glows: " + blessed.getName());
            }
            case "3" -> {
                if (!spendScore(gearCost)) {
                    view.showWarning("Not enough score.");
                    return;
                }
                Item armor = firstEquippedArmor();
                if (armor == null) {
                    view.showWarning("Equip armor first.");
                    return;
                }
                Item blessed = blessItem(armor, false);
                player.addItem(blessed);
                player.equipArmor(blessed);
                view.showSuccess("Your armor is blessed: " + blessed.getName());
            }
            default -> {
                return;
            }
        }
        room.setCompleted(true);
        room.setExplored(true);
    }

    private boolean spendScore(int cost) {
        if (player.getTotalScore() < cost) {
            return false;
        }
        player.addScore(-cost);
        return true;
    }

    /**
     * Determine appropriate rarity tier for equipment based on player level during
     * save restoration.
     * 
     * @param level Player's current level
     * @return Appropriate Rarity for loaded equipment
     */
    private Rarity determineSaveRarity(int level) {
        if (level >= 10) {
            return Rarity.RARE;
        } else if (level >= 5) {
            return Rarity.UNCOMMON;
        } else {
            return Rarity.COMMON;
        }
    }

    private Item blessItem(Item original, boolean isWeapon) { // this isn't super smart or cool, but it works
        Rarity bumped = bumpRarity(original.getRarity());
        String newName = original.getName() + " (Blessed)";
        Item blessed = new Item(newName, original.getType(), original.getElement(), bumped);
        // Small extra bonus
        if (isWeapon) {
            // Slightly higher than rarity alone
            blessed = new Item(newName, original.getType(), original.getElement(), bumped);
        }
        return blessed;
    }

    private Item firstEquippedArmor() {
        if (player.getEquippedHelmet() != null)
            return player.getEquippedHelmet();
        if (player.getEquippedChestplate() != null)
            return player.getEquippedChestplate();
        if (player.getEquippedLeggings() != null)
            return player.getEquippedLeggings();
        if (player.getEquippedBoots() != null)
            return player.getEquippedBoots();
        return null;
    }

    private Rarity bumpRarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Rarity.UNCOMMON;
            case UNCOMMON -> Rarity.RARE;
            case RARE -> Rarity.MYTHICAL;
            case MYTHICAL -> Rarity.LEGENDARY;
            case LEGENDARY -> Rarity.LEGENDARY;
            case DEBUG -> Rarity.COMMON;
        };
    }
    // That DEBUG can be downgraded to Common is funny

    private void handleInventory() {
        view.clear();
        view.printCentered("INVENTORY");
        view.printLine("");

        if (player.getInventory().size() == 0) {
            view.printLine("Inventory is empty");
            view.pressAnyKey();
            return;
        }

        boolean done = false;
        while (!done) {
            view.clear();
            view.printCentered("INVENTORY");
            view.printLine("");

            // Organize inventory by type
            DynArray<Item> weapons = new DynArray<>();
            DynArray<Item> armor = new DynArray<>();
            DynArray<Item> potions = new DynArray<>();
            DynArray<Item> other = new DynArray<>();

            for (int i = 0; i < player.getInventory().size(); i++) {
                Item item = player.getInventory().get(i);
                if (item.getType().isWeapon()) {
                    weapons.add(item);
                } else if (item.getType().isArmor()) {
                    armor.add(item);
                } else if (item.getType().isPotion()) {
                    // Mana disabled: skip adding Mana potions to potion listing
                    if (item.getType() != ItemType.POTION_MANA) {
                        potions.add(item);
                    }
                } else {
                    other.add(item);
                }
            }

            // Display weapons section
            if (weapons.size() > 0) {
                view.printLine(ConsoleColor.BRIGHT_RED + "═══ WEAPONS ═══" + ConsoleColor.RESET);
                for (int i = 0; i < weapons.size(); i++) {
                    displayInventoryItem(weapons.get(i), player.getInventory().indexOf(weapons.get(i)));
                }
                view.printLine("");
            }

            // Display armor section
            if (armor.size() > 0) {
                view.printLine(ConsoleColor.BRIGHT_YELLOW + "═══ ARMOR ═══" + ConsoleColor.RESET);
                for (int i = 0; i < armor.size(); i++) {
                    displayInventoryItem(armor.get(i), player.getInventory().indexOf(armor.get(i)));
                }
                view.printLine("");
            }

            // Display potions section
            if (potions.size() > 0) {
                view.printLine(ConsoleColor.BRIGHT_MAGENTA + "═══ POTIONS & CONSUMABLES ═══" + ConsoleColor.RESET);
                for (int i = 0; i < potions.size(); i++) {
                    displayInventoryItem(potions.get(i), player.getInventory().indexOf(potions.get(i)));
                }
                view.printLine("");
            }

            // Display other items section
            if (other.size() > 0) {
                view.printLine(ConsoleColor.BRIGHT_CYAN + "═══ OTHER ITEMS ═══" + ConsoleColor.RESET);
                for (int i = 0; i < other.size(); i++) {
                    displayInventoryItem(other.get(i), player.getInventory().indexOf(other.get(i)));
                }
                view.printLine("");
            }

            view.printLine(ConsoleColor.BRIGHT_GREEN + "↑" + ConsoleColor.RESET + " = Better  " +
                    ConsoleColor.RED + "↓" + ConsoleColor.RESET + " = Worse");
            view.printLine("Quick: [#e] Equip | [#u] Use | [#d] Drop | [A] Auto-Equip Best");
            view.printLine("Select item number to inspect/equip, or 0 to exit:");
            view.print("> ");

            String input = view.readLine().trim();

            // Check for auto-equip command
            if (input.equalsIgnoreCase("a") || input.equalsIgnoreCase("auto")) {
                autoEquipBestGear();
                view.pressAnyKey();
                continue;
            }

            // Check for quick commands (e.g., "4e", "3u", "5d")
            if (input.length() >= 2) {
                char lastChar = input.charAt(input.length() - 1);
                String numberPart = input.substring(0, input.length() - 1);

                try {
                    int itemIndex = Integer.parseInt(numberPart) - 1;

                    if (itemIndex >= 0 && itemIndex < player.getInventory().size()) {
                        Item quickItem = player.getInventory().get(itemIndex);

                        // Quick equip command
                        if (lastChar == 'e' || lastChar == 'E') {
                            if (quickItem.getType().isWeapon()) {
                                player.equipWeapon(quickItem);
                                view.showSuccess("✓ Equipped: " + quickItem.getName());
                                view.pressAnyKey();
                                continue;
                            } else if (quickItem.getType().isArmor()) {
                                player.equipArmor(quickItem);
                                view.showSuccess("✓ Equipped: " + quickItem.getName());
                                view.pressAnyKey();
                                continue;
                            } else {
                                view.showWarning("⚠ Cannot equip " + quickItem.getName());
                                view.pressAnyKey();
                                continue;
                            }
                        }

                        // Quick use command (for potions)
                        if (lastChar == 'u' || lastChar == 'U') {
                            if (quickItem.getType().isPotion()) {
                                handleQuickUsePotion(quickItem);
                                player.getInventory().remove(itemIndex);
                                view.pressAnyKey();
                                continue;
                            } else {
                                view.showWarning("⚠ Cannot use " + quickItem.getName());
                                view.pressAnyKey();
                                continue;
                            }
                        }

                        // Quick drop command
                        if (lastChar == 'd' || lastChar == 'D') {
                            // Confirm drop for equipped items
                            boolean isEquipped = (quickItem == player.getEquippedWeapon() ||
                                    quickItem == player.getEquippedHelmet() ||
                                    quickItem == player.getEquippedChestplate() ||
                                    quickItem == player.getEquippedLeggings() ||
                                    quickItem == player.getEquippedBoots());

                            if (isEquipped) {
                                view.showWarning("⚠ Cannot drop equipped items!");
                                view.pressAnyKey();
                                continue;
                            }

                            player.getInventory().remove(itemIndex);
                            view.showSuccess("✓ Dropped: " + quickItem.getName());
                            view.pressAnyKey();
                            continue;
                        }
                    }
                } catch (NumberFormatException e) {
                    // deleted
                }
            }

            // Normal item selection
            // Parse input as item number
            try {
                int choice = Integer.parseInt(input);
                if (choice == 0) {
                    done = true;
                    continue;
                }
                if (choice < 1 || choice > player.getInventory().size()) {
                    view.showWarning("Invalid choice");
                    view.pressAnyKey();
                    continue;
                }

                Item selected = player.getInventory().get(choice - 1);
                view.clear();
                view.printLine(selected.getDetailLine());
                view.printLine("");

                if (selected.getType().isWeapon() || selected.getType().isArmor()) {
                    view.printLine("[E] Equip  |  [B] Back");
                    view.print("> ");
                    String act = view.readLine().trim().toUpperCase();
                    if ("E".equals(act)) {
                        if (selected.getType().isWeapon()) {
                            player.equipWeapon(selected);
                            view.showSuccess("Equipped weapon: " + selected.getName());
                        } else {
                            player.equipArmor(selected);
                            view.showSuccess("Equipped armor: " + selected.getName());
                        }
                    }
                } else {
                    view.printLine("[B] Back");
                    view.readLine();
                }
            } catch (NumberFormatException e) {
                view.showWarning("Please enter a number");
                view.pressAnyKey();
            }
        }
    }

    // Helper method to get equipped armor piece by type
    private Item getEquippedArmorOfType(ItemType type) {
        return switch (type) {
            case ARMOR_HELMET -> player.getEquippedHelmet();
            case ARMOR_CHESTPLATE -> player.getEquippedChestplate();
            case ARMOR_LEGGINGS -> player.getEquippedLeggings();
            case ARMOR_BOOTS -> player.getEquippedBoots();
            default -> null;
        };
    }

    // Helper method to display individual inventory items with comparison arrows
    private void displayInventoryItem(Item item, int index) {
        String equippedTag = "";
        String colorSuffix = ConsoleColor.RESET;
        String upgradeIndicator = "";
        String itemColor = ConsoleColor.RESET;

        // Determine color based on item type
        if (item.getType().isWeapon()) {
            itemColor = ConsoleColor.BRIGHT_RED; // Weapons in red
            // Check if it's better than equipped weapon
            if (player.getEquippedWeapon() != null && item != player.getEquippedWeapon()) {
                if (item.getDamage() > player.getEquippedWeapon().getDamage()) {
                    upgradeIndicator = " " + ConsoleColor.BRIGHT_GREEN + "↑" + itemColor; // Better
                } else if (item.getDamage() < player.getEquippedWeapon().getDamage()) {
                    upgradeIndicator = " " + ConsoleColor.RED + "↓" + itemColor; // Worse
                }
            }
        } else if (item.getType().isArmor()) {
            itemColor = ConsoleColor.BRIGHT_YELLOW; // Armor in yellow
            // Check if it's better than equipped armor of same type
            Item equippedPiece = getEquippedArmorOfType(item.getType());
            if (equippedPiece != null && item != equippedPiece) {
                if (item.getDefense() > equippedPiece.getDefense()) {
                    upgradeIndicator = " " + ConsoleColor.BRIGHT_GREEN + "↑" + itemColor; // Better
                } else if (item.getDefense() < equippedPiece.getDefense()) {
                    upgradeIndicator = " " + ConsoleColor.RED + "↓" + itemColor; // Worse
                }
            }
        } else if (item.getType().isPotion()) {
            itemColor = ConsoleColor.BRIGHT_MAGENTA; // Potions in magenta
        } else {
            itemColor = ConsoleColor.BRIGHT_CYAN; // Other items in cyan
        }

        // Mark equipped items
        if (item == player.getEquippedWeapon()) {
            equippedTag = " " + ConsoleColor.BRIGHT_WHITE + "[EQUIPPED]" + itemColor;
        } else if (item == player.getEquippedHelmet() || item == player.getEquippedChestplate()
                || item == player.getEquippedLeggings()
                || item == player.getEquippedBoots()) {
            equippedTag = " " + ConsoleColor.BRIGHT_WHITE + "[EQUIPPED]" + itemColor;
        }

        view.printLine(itemColor + "[" + (index + 1) + "] " + item.getDetailLine() + upgradeIndicator
                + equippedTag + colorSuffix);
    }

    private void autoEquipBestGear() {
        view.clear();
        view.printLine(
                ConsoleColor.BRIGHT_CYAN + "╔════════════════════════════════════════════════╗" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BRIGHT_YELLOW
                + "           AUTO-EQUIP BEST GEAR                 " + ConsoleColor.BRIGHT_CYAN + "║"
                + ConsoleColor.RESET);
        view.printLine(
                ConsoleColor.BRIGHT_CYAN + "╚════════════════════════════════════════════════╝" + ConsoleColor.RESET);
        view.printLine(""); // this part is not necessary but I was having issues with errors at first, so
                            // this helps a little.

        int equipCount = 0;

        // Find best weapon
        Item bestWeapon = null;
        int bestWeaponDmg = player.getEquippedWeapon() != null ? player.getEquippedWeapon().getDamage() : 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getType().isWeapon() && item.getDamage() > bestWeaponDmg) {
                bestWeapon = item;
                bestWeaponDmg = item.getDamage();
            }
        }

        if (bestWeapon != null && bestWeapon != player.getEquippedWeapon()) {
            player.equipWeapon(bestWeapon);
            view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ Equipped weapon: " + bestWeapon.getName() + " (DMG: "
                    + bestWeapon.getDamage() + ")" + ConsoleColor.RESET);
            equipCount++;
        }

        // Find best armor for each slot
        ItemType[] armorSlots = {
                ItemType.ARMOR_HELMET,
                ItemType.ARMOR_CHESTPLATE,
                ItemType.ARMOR_LEGGINGS,
                ItemType.ARMOR_BOOTS
        };

        for (ItemType slotType : armorSlots) {
            Item bestArmor = null;
            int bestArmorDef = 0;
            Item currentEquipped = getEquippedArmorOfType(slotType);

            if (currentEquipped != null) {
                bestArmorDef = currentEquipped.getDefense();
            }

            logger.debug("INVENTORY", "Checking slot " + slotType + ": currentEquipped="
                    + (currentEquipped != null ? currentEquipped.getName() : "NONE") + ", startDef=" + bestArmorDef);

            for (int i = 0; i < player.getInventory().size(); i++) {
                Item item = player.getInventory().get(i);
                if (item.getType() == slotType && item.getDefense() > bestArmorDef) {
                    bestArmor = item;
                    bestArmorDef = item.getDefense();
                    logger.debug("INVENTORY",
                            "  Found better " + slotType + ": " + item.getName() + " (DEF: " + item.getDefense() + ")");
                }
            }

            if (bestArmor != null && bestArmor != currentEquipped) {
                player.equipArmor(bestArmor);
                String slotName = slotType.toString().replace("ARMOR_", "").toLowerCase();
                slotName = slotName.substring(0, 1).toUpperCase() + slotName.substring(1);
                view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ Equipped " + slotName + ": " + bestArmor.getName()
                        + " (DEF: " + bestArmor.getDefense() + ")" + ConsoleColor.RESET);
                equipCount++;
            } else if (currentEquipped == null && bestArmor != null) {
                // Fallback: If nothing is equipped and we found something, equip it
                player.equipArmor(bestArmor);
                String slotName = slotType.toString().replace("ARMOR_", "").toLowerCase();
                slotName = slotName.substring(0, 1).toUpperCase() + slotName.substring(1);
                view.printLine(ConsoleColor.BRIGHT_GREEN + "✓ Equipped " + slotName + ": " + bestArmor.getName()
                        + " (DEF: " + bestArmor.getDefense() + ")" + ConsoleColor.RESET);
                equipCount++;
            }
        }

        view.printLine(""); // I dislike inventory management and there's a lot of stuff to look at and
                            // there's no real limit to inventory size. Overight? perhaps.
        if (equipCount == 0) {
            view.printLine(ConsoleColor.BRIGHT_YELLOW + "⚠ Already wearing the best gear!" + ConsoleColor.RESET);
        } else {
            view.printLine(
                    ConsoleColor.BRIGHT_GREEN + "✓ Auto-equipped " + equipCount + " item(s)!" + ConsoleColor.RESET);
        }

        logger.info("INVENTORY", "Auto-equipped " + equipCount + " items");
    }

    // The whole lore system isn't exactly necessary; this system was designed at
    // first to store Gate Addresses when I was working on making a RPG Extraction
    // Crawler inspired by the StarGate franchise.
    private void handleLoreCollection() {
        view.clear();
        view.printCentered("═══ LORE COLLECTION ═══");
        view.printLine("");

        if (player.getLoreCollection().size() == 0) {
            view.printLine("You haven't discovered any lore items yet.");
            view.printLine("Explore treasure rooms to find fragments of Daedalus's story.");
        } else {
            view.printLine("Lore items found: " + player.getLoreCollection().size() + "/10");
            view.printLine("");

            for (int i = 0; i < player.getLoreCollection().size(); i++) {
                view.printLine("[" + (i + 1) + "] " + player.getLoreCollection().get(i).getName());
            }

            view.printLine("");
            view.printLine("Enter number to read, or 0 to go back:");
            view.print("> ");

            String input = view.readLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= player.getLoreCollection().size()) {
                    view.clear();
                    view.printLine(player.getLoreCollection().get(choice - 1).getLoreText());
                    view.printLine("");
                    view.pressAnyKey();
                    handleLoreCollection(); // Show menu again
                }
            } catch (NumberFormatException e) {
                // Invalid input, just return
            }
        }

        view.pressAnyKey();
    }

    private void handleCrafting() {
        CraftingSystem crafting = new CraftingSystem(player, view);
        crafting.openCraftingMenu();
    } // Crafting System. See other file for details. But quite cool.

    private void handleSaveGame() {
        handleSaveGame(false);
    }

    private void handleSaveGame(boolean costAlreadyPaid) {
        logger.info("SAVE", "Player initiated save");
        view.printLine("");

        // Only allow saving from Safe Rooms unless paying SAVE_QUIT_SCORE_COST score
        Room curRoom = currentFloor.getRoomAt(currentFloor.getPlayerX(), currentFloor.getPlayerY());
        if (curRoom != null && curRoom.getType() == Room.RoomType.SAFE) {
            boolean success = saveSystem.saveGame(player, currentFloorNumber, runSeed);
            if (success) {
                view.showSuccess("Game saved successfully!");
            } else {
                view.showError("Failed to save game!");
            }
            view.pressAnyKey();
            return;
        }

        // If cost was already paid (e.g., from quit menu), just save directly
        if (costAlreadyPaid) {
            boolean success = saveSystem.saveGame(player, currentFloorNumber, runSeed);
            if (success) {
                view.showSuccess("Game saved successfully (paid " + SAVE_QUIT_SCORE_COST + " score)!");
            } else {
                view.showError("Failed to save game!");
            }
            return;
        }

        // Is this a good feature ? No
        // Did I spend way too long implementing it ? Yes
        // But hey, it works.
        view.showWarning("Saving is only allowed in Safe Rooms.");
        view.printLine("Options: [P] Pay " + SAVE_QUIT_SCORE_COST
                + " score to save now | [H] Highlight nearest Safe Room | [B] Back");
        view.print("> ");
        String sChoice = view.readLine().trim();
        if (sChoice.equalsIgnoreCase("p")) {
            if (spendScore(SAVE_QUIT_SCORE_COST)) {
                boolean success = saveSystem.saveGame(player, currentFloorNumber, runSeed);
                if (success) {
                    view.showSuccess("Game saved successfully (paid " + SAVE_QUIT_SCORE_COST + " score)!");
                } else {
                    view.showError("Failed to save game!"); // Hopefully shouldn't happen
                }
            } else {
                view.showWarning("Not enough score (" + SAVE_QUIT_SCORE_COST + " required).");
            }
            view.pressAnyKey();
        } else if (sChoice.equalsIgnoreCase("h")) {
            highlightNearestSafeRoomOnMap();
        } else {
            // Back - do nothing
        }
    }

    private void handleMap() {
        view.clear();
        view.printLine("Floor Map:");
        // Set player level and dungeon floor for dynamic monster coloring
        view.getMapView().setPlayerLevel(player.getLevel());
        view.getMapView().setDungeonFloor(currentFloor);
        // Show the full floor map (revealed) when player requests the map.
        view.getMapView().showFullDungeonMap(
                currentFloor.getMap(), // full map array
                currentFloor.getVisibleMask(), // visibility mask (unused for full reveal)
                currentFloor.getExploredMask(), // explored mask (unused for full reveal)
                currentFloor.getPlayerX(), // Player X position
                currentFloor.getPlayerY(), // Player Y position
                player.hasTorch()); // Whether player has torch (affects trap visibility)
        view.pressAnyKey();
    } // This looks a lot more complicated than it actually is.

    // Find the nearest Safe Room by Manhattan distance
    // Using Manhattan system because it's easier - arguably more complicated and I
    // could have made a method, but oh well.
    // Perhaps I ought to learn about System Design & planning
    private Room findNearestSafeRoom() {
        Room nearest = null;
        int bestDist = Integer.MAX_VALUE;
        int px = currentFloor.getPlayerX();
        int py = currentFloor.getPlayerY();
        for (int i = 0; i < currentFloor.getRooms().size(); i++) {
            Room room = currentFloor.getRooms().get(i);
            if (room.getType() != Room.RoomType.SAFE)
                continue;
            int cx = room.getX() + room.getWidth() / 2;
            int cy = room.getY() + room.getHeight() / 2;
            int dist = Math.abs(px - cx) + Math.abs(py - cy);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = room;
            }
        }
        return nearest;
    }

    // Temporarily highlight the nearest Safe Room on the map and show it
    // Sometimes doesn't work; todo check if safe rooms are actually generated
    private void highlightNearestSafeRoomOnMap() {
        Room nearest = findNearestSafeRoom();
        if (nearest == null) {
            view.showWarning("No Safe Rooms found on this floor.");
            view.pressAnyKey();
            return;
        }

        int cx = nearest.getX() + nearest.getWidth() / 2;
        int cy = nearest.getY() + nearest.getHeight() / 2;

        char[][] map = currentFloor.getMap();
        boolean[][] vis = currentFloor.getVisibleMask();
        boolean[][] exp = currentFloor.getExploredMask();

        // Save previous state
        char prevTile = map[cy][cx];
        boolean prevVis = vis[cy][cx];
        boolean prevExp = exp[cy][cx];

        // Overlay highlight and ensure it's visible
        map[cy][cx] = '*';
        vis[cy][cx] = true;
        exp[cy][cx] = true;

        view.clear();
        view.printLine(ConsoleColor.BRIGHT_CYAN + "Nearest Safe Room highlighted with '*'" + ConsoleColor.RESET);
        view.printLine("(Approx. distance: "
                + (Math.abs(currentFloor.getPlayerX() - cx) + Math.abs(currentFloor.getPlayerY() - cy)) + ")");
        // Set player level and dungeon floor for dynamic monster coloring
        view.getMapView().setPlayerLevel(player.getLevel());
        view.getMapView().setDungeonFloor(currentFloor);
        view.showDungeonMap(map, vis, exp, currentFloor.getPlayerX(), currentFloor.getPlayerY(), player.hasTorch());
        view.pressAnyKey();

        // Restore previous state
        map[cy][cx] = prevTile;
        vis[cy][cx] = prevVis;
        exp[cy][cx] = prevExp;
    }

    // One of the most painful things to write. Technically not exactly up to date
    // because some things are no longer used or whatever, but I've spent too much
    // time on other systems.
    private void handleHelp() {
        view.clear();
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╔═══════════════════════════════════════════════════════════════════════════╗" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BRIGHT_YELLOW
                + "                           GAME HELP & LEGEND                              "
                + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╠═══════════════════════════════════════════════════════════════════════════╣" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BOLD
                + "  CONTROLS:                                                                  " + ConsoleColor.RESET
                + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [W/A/S/D] - Move up/left/down/right                                      ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [E]       - Interact with objects (chests, doors, NPCs)                  ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [I]       - Open inventory                                               ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [1/3]     - Quick-use Health/Stamina potion                               ║"
                + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [L]       - View lore collection                                         ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [C]       - Open crafting menu                                           ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [V]       - Save game                                                    ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [M]       - View full map with legend                                    ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [H]       - Show this help screen                                        ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [Q]       - Quit current floor                                           ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╠═══════════════════════════════════════════════════════════════════════════╣" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BOLD
                + "  SYMBOLS & ICONS:                                                           " + ConsoleColor.RESET
                + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_GREEN + "✓" + ConsoleColor.RESET
                + " - Success message                                                      " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_RED + "✖" + ConsoleColor.RESET
                + " - Error or failure message                                             " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_YELLOW + "⚠" + ConsoleColor.RESET
                + " - Warning or caution                                                   " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_GREEN + "➤" + ConsoleColor.RESET
                + " - Action taken (combat moves)                                          " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_YELLOW + "⚡" + ConsoleColor.RESET
                + " - Elemental bonus or combo                                             " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_YELLOW + "⭐" + ConsoleColor.RESET
                + " - Important item or high score                                         " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_YELLOW + "🏆" + ConsoleColor.RESET
                + " - Achievement unlocked                                                 " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_RED + "⚔" + ConsoleColor.RESET
                + "  - Combat or offensive action                                           " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_BLUE + "🛡" + ConsoleColor.RESET
                + "  - Defense or defensive action                                          " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.RED + "💀" + ConsoleColor.RESET
                + " - Death, corpse, or defeated enemy                                     " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╠═══════════════════════════════════════════════════════════════════════════╣" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BOLD
                + "  MAP SYMBOLS:                                                               " + ConsoleColor.RESET
                + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_YELLOW + "@" + ConsoleColor.RESET
                + " - You (the player)                                                     " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.WHITE + "#" + ConsoleColor.RESET
                + " - Wall                                                                 " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_BLACK + "." + ConsoleColor.RESET
                + " - Floor (walkable)                                                     " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.YELLOW + "+" + ConsoleColor.RESET
                + " - Closed door                                                          " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_GREEN + "/" + ConsoleColor.RESET
                + " - Open door                                                            " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.YELLOW + ">" + ConsoleColor.RESET
                + " - Stairs down (next floor)                                             " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_RED + "^" + ConsoleColor.RESET
                + " - Trap (danger!)                                                       " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.YELLOW + "C" + ConsoleColor.RESET
                + " - Chest (treasure)                                                     " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_MAGENTA + "?" + ConsoleColor.RESET
                + " - Puzzle or mystery                                                    " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.YELLOW + "$" + ConsoleColor.RESET
                + " - Shop/merchant                                                        " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_CYAN + "&" + ConsoleColor.RESET
                + " - Altar/safe zone                                                      " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.GREEN + "g,o,s,r" + ConsoleColor.RESET
                + " - Common monsters                                                  " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.CYAN + "T,Z" + ConsoleColor.RESET
                + " - Uncommon monsters                                                    " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.MAGENTA + "V" + ConsoleColor.RESET
                + " - Rare monster                                                         " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║  " + ConsoleColor.BRIGHT_RED + "D" + ConsoleColor.RESET
                + " - Boss monster                                                         " + ConsoleColor.BRIGHT_CYAN
                + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╠═══════════════════════════════════════════════════════════════════════════╣" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.BOLD
                + "  COMBAT INFO:                                                               " + ConsoleColor.RESET
                + ConsoleColor.BRIGHT_CYAN + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  HP - Health Points (when 0, you die)                                     ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  ST - Stamina (needed to use abilities)                                   ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [BLEED] - Damage over time effect                                        ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [VAMPIRIC] - Heals you when dealing damage                               ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [EXECUTE] - Extra damage to low-health enemies                           ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  [SWIFT] - Restores stamina when attacking                                ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "║  COMBO - Build up by using attacking abilities                            ║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_CYAN
                + "╚═══════════════════════════════════════════════════════════════════════════╝" + ConsoleColor.RESET);
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_GREEN + "TIP: Press [M] to view the full map with detailed legend!"
                + ConsoleColor.RESET);
        view.printLine("");
        view.pressAnyKey();
    }

    private void openDebugConsole() {
        logger.info("DEBUG", "Debug console opened");
        boolean inDebugConsole = true;

        while (inDebugConsole) {
            view.clear();
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "╔═══════════════════════════════════════════════════════════════════════════╗"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA + "║" + ConsoleColor.BRIGHT_YELLOW
                    + "                            DEBUG CONSOLE                                 "
                    + ConsoleColor.BRIGHT_MAGENTA + "║" + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "╠═══════════════════════════════════════════════════════════════════════════╣"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  Enter debug commands (prefix with '/')                                  ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║                                                                           ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  /help       - Show all debug commands                                    ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  /stats      - Show detailed player stats                                 ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  /heal       - Fully heal player                                          ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  /level <n>  - Gain N levels                                              ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  /reveal     - Reveal entire map                                          ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "║  exit        - Exit debug console                                         ║"
                    + ConsoleColor.RESET);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA
                    + "╚═══════════════════════════════════════════════════════════════════════════╝"
                    + ConsoleColor.RESET);
            view.printLine("");
            view.print(ConsoleColor.BRIGHT_MAGENTA + "DEBUG > " + ConsoleColor.RESET);

            try {
                String command = view.readLine().trim();

                if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")
                        || command.equalsIgnoreCase("/exit")) {
                    logger.info("DEBUG", "Exiting debug console");
                    inDebugConsole = false;
                    continue;
                }

                if (command.isEmpty()) {
                    continue;
                }

                // Add '/' prefix if not present
                if (!command.startsWith("/")) {
                    command = "/" + command;
                }

                // Execute the debug command
                debugSystem.executeCommand(command);
                view.printLine("");
                view.pressAnyKey();

            } catch (Exception e) {
                logger.error("DEBUG", "Error in debug console", e);
                view.showError("Debug console error: " + e.getMessage());
                view.pressAnyKey();
            }
        }

        logger.info("DEBUG", "Debug console closed");
    }

    private void checkBossRoar() {
        // Boss roars every X actions based on difficulty
        Difficulty difficulty = currentFloor.getDifficulty();
        int roarFrequency = switch (difficulty) {
            case EASY -> 9;
            case NORMAL -> 7;
            case HARD -> 5;
            case NIGHTMARE -> 3;
            case HELL -> 1;
            case TUTORIAL -> 12;
        };

        if (currentFloor.getActionCounter() > 0 && currentFloor.getActionCounter() % roarFrequency == 0) {
            // Boss "roars" - reveal position temporarily
            // Inspired by Bats
            currentFloor.setBossRevealed(true);
            view.printLine("");
            view.showWarning(
                    "You hear a roar gently shake the walls and floor of the Dungeon... the Boss is aware of you...");
            view.printLine("");

            // Calculate reveal duration based on player intelligence
            int intelligenceBonus = Math.min(player.getIntelligence() / BOSS_ROAR_INTELLIGENCE_DIVISOR,
                    BOSS_ROAR_MAX_INTELLIGENCE_BONUS);
            int maxRevealDuration = Math.min(BOSS_ROAR_BASE_REVEAL_DURATION + intelligenceBonus,
                    BOSS_ROAR_MAX_REVEAL_DURATION);

            // Store when boss should be hidden again
            currentFloor.setBossRevealExpires(currentFloor.getActionCounter() + maxRevealDuration);
        }

        // Check if reveal duration has expired
        if (currentFloor.isBossRevealed() && currentFloor.getActionCounter() >= currentFloor.getBossRevealExpires()) {
            currentFloor.setBossRevealed(false);
        }

        // Boss moves toward player if revealed (or periodically)
        if (currentFloor.isBossRevealed() || currentFloor.getActionCounter() % BOSS_MOVE_FREQUENCY == 0) {
            moveBossTowardPlayer();
        }
    }

    private void moveBossTowardPlayer() {
        int bossX = currentFloor.getBossX();
        int bossY = currentFloor.getBossY();
        int playerX = currentFloor.getPlayerX();
        int playerY = currentFloor.getPlayerY();

        // Simple pathfinding: move one step toward player (Manhattan distance)
        int newBossX = bossX;
        int newBossY = bossY;

        // Prioritize vertical movement if there's more vertical distance
        if (Math.abs(playerY - bossY) > Math.abs(playerX - bossX)) {
            newBossY += (playerY > bossY) ? 1 : -1;
        } else {
            newBossX += (playerX > bossX) ? 1 : -1;
        }

        // Check if new position is valid (within floor bounds and not a wall)
        if (currentFloor.isValidFloor(newBossX, newBossY)) {
            currentFloor.setBossPosition(newBossX, newBossY);
        }
    }

    private void gameOver() {
        view.clear();
        if (player.isAlive()) {
            view.showSuccess("=== GAME WON ==="); // there's no real "winning the game", this is here to prevent errors!
        } else {
            view.showError("=== GAME OVER ===");
        }

        view.printLine("");
        view.printLine(player.toString());
        view.printLine("Final Score: " + player.getTotalScore());
        view.printLine("");

        // Save to leaderboard
        LeaderboardSystem.addEntry(player.getName(), player.getTotalScore(), player.getLevel(),
            player.getExperience(), player.getMonstersDefeated(), runSeed);

        // Show if this was a top score
        if (LeaderboardSystem.isTopScore(player.getTotalScore())) {
            view.printColoredLine("New High Score!", ConsoleColor.BRIGHT_YELLOW);
        }

        view.printLine("");
        view.pressAnyKey();
        running = false;
    }

    /**
     * Validates and sanitizes player name input.
     * 
     * @param name The raw name input from user
     * @return A valid player name (default name if empty, truncated if too long)
     */
    private String validatePlayerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.info("GAME", "Empty name; using default: " + DEFAULT_PLAYER_NAME);
            return DEFAULT_PLAYER_NAME;
        }

        // Sanitize input: remove control characters and limit to printable ASCII
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();

        // Remove potentially problematic characters
        name = name.replaceAll("[<>|\"\\\\/:*?]", "");

        // Prevent SQL-injection-like patterns (even though we don't use SQL)
        name = name.replaceAll("(?i)(DROP|DELETE|INSERT|UPDATE|SELECT|;|--)", "");

        // If nothing left after sanitization, use default
        if (name.isEmpty()) {
            logger.warn("GAME", "Name contained only invalid characters; using default");
            return DEFAULT_PLAYER_NAME;
        }

        if (name.length() > DEFAULT_PLAYER_NAME_MAX_LENGTH) {
            name = name.substring(0, DEFAULT_PLAYER_NAME_MAX_LENGTH);
            view.showWarning("Name truncated to " + DEFAULT_PLAYER_NAME_MAX_LENGTH + " characters: " + name);
            logger.info("GAME", "Player name truncated: " + name);
        }

        return name;
    }

    /**
     * Parses user input as an integer with error handling.
     * 
     * @param input        The string to parse
     * @param defaultValue Value to return if parsing fails
     * @return Parsed integer or default value
     */
    private int parseIntOrDefault(String input, int defaultValue) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            logger.debug("INPUT", "Failed to parse integer: " + input);
            return defaultValue;
        }
    }

    /**
     * Validates if a choice is within valid range.
     * 
     * @param choice The choice to validate
     * @param min    Minimum valid value (inclusive)
     * @param max    Maximum valid value (inclusive)
     * @return true if valid, false otherwise
     */
    private boolean isValidChoice(int choice, int min, int max) {
        return choice >= min && choice <= max;
    }

    /**
     * Safety check: Warns player if HP is critically low.
     * Called before dangerous actions like entering combat.
     */
    private void checkLowHealthWarning() {
        if (!player.isAlive())
            return;

        int hpPercent = (player.getHealth() * 100) / player.getMaxHealth();
        if (hpPercent <= MIN_SAFE_HP_PERCENT) {
            view.printLine("");
            view.printColoredLine("⚠ WARNING: Health critically low (" + hpPercent + "%)!", ConsoleColor.BRIGHT_RED);
            view.printColoredLine("Consider using potions or finding a safe room.", ConsoleColor.YELLOW);
            view.printLine("");
        }
    }

    /**
     * Safety check: Prevents inventory overflow.
     * Returns true if item can be added, false if inventory is full.
     */
    private boolean canAddToInventory() {
        if (player.getInventory().size() >= MAX_INVENTORY_SIZE) {
            view.showError("Inventory full! (Max: " + MAX_INVENTORY_SIZE + " items)");
            view.printColoredLine("Consider dropping or selling items.", ConsoleColor.YELLOW);
            logger.warn("INVENTORY", "Inventory overflow prevented");
            return false;
        }
        return true;
    }

    /**
     * Auto-save system: Saves game every few floors automatically.
     * Provides backup in case of crashes or unexpected exits.
     */
    private void checkAutoSave() {
        floorsCompletedSinceLastSave++;
        if (floorsCompletedSinceLastSave >= AUTO_SAVE_INTERVAL) {
            try {
                saveSystem.saveGame(player, currentFloorNumber, runSeed);
                view.printColoredLine("✓ Auto-saved (Floor " + currentFloorNumber + ")", ConsoleColor.BRIGHT_GREEN);
                logger.info("AUTOSAVE", "Game auto-saved at floor " + currentFloorNumber);
                floorsCompletedSinceLastSave = 0;
            } catch (Exception e) {
                logger.error("AUTOSAVE", "Auto-save failed", e);
                // Don't show error to player - it's just a backup
            }
        }
    }

    /**
     * Input validation helper: Tracks failed inputs and shows help after too many
     * failures.
     */
    private void handleInvalidInput(String input) {
        consecutiveFailedInputs++;
        view.showError("Invalid input: " + input);

        if (consecutiveFailedInputs >= MAX_FAILED_INPUTS) {
            view.printLine("");
            view.printColoredLine("Having trouble? Here are the available commands:", ConsoleColor.BRIGHT_CYAN);
            view.printLine("  W/A/S/D - Move around");
            view.printLine("  E - Interact with objects");
            view.printLine("  I - Open inventory");
            view.printLine("  H - Show help menu");
            view.printLine("");
            consecutiveFailedInputs = 0; // Reset counter
        }
    }

    /**
     * Resets failed input counter on successful input.
     */
    private void resetInputFailureCounter() {
        consecutiveFailedInputs = 0;
    }

    /**
     * Handles combat aftermath including level up checks and stat allocation.
     * 
     * @param combat The completed combat system
     * @param room   The room where combat took place (marked as completed if player
     *               survives)
     * @return true if player survived, false otherwise
     */
    private boolean handleCombatResult(CombatSystem combat, Room room) {
        // Check for level up and stat allocation
        if (combat.didPlayerLevelUp()) {
            view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
            view.showStatAllocation(player);
        }

        if (player.isAlive()) {
            room.setCompleted(true);
            room.setExplored(true);
            return true;
        }
        return false;
    }

    /**
     * Gets the potion type name for display purposes.
     * 
     * @param potionType The potion item type
     * @return Human-readable potion name
     */
    private String getPotionTypeName(ItemType potionType) {
        return switch (potionType) {
            case POTION_HEALTH -> "Health";
            case POTION_STAMINA -> "Stamina";
            default -> "Unknown";
        };
    }

    /**
     * Checks if player leveled up and displays stat allocation screen.
     * 
     * @param combat The combat system to check for level up
     */
    private void checkAndDisplayLevelUp(CombatSystem combat) {
        if (combat.didPlayerLevelUp()) {
            view.showSuccess("LEVEL UP! You reached level " + player.getLevel() + "!");
            view.showStatAllocation(player);
        }
    }

    /**
     * Displays an introductory cutscene explaining the dungeon and player's
     * objective.
     * Called once at the start of a new game after character creation.
     */
    private void showIntroCutscene() {
        view.clear();
        view.printLine("");
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "═══════════════════════════════════════════════════════"
                + ConsoleColor.RESET);
        view.printCentered(ConsoleColor.BRIGHT_YELLOW + "Daedalus' Infinite Dungeon" + ConsoleColor.RESET);
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "═══════════════════════════════════════════════════════"
                + ConsoleColor.RESET);
        view.printLine("");
        view.printLine("");

        // Story introduction with typing effect
        String[] introLines = {
                "You awaken in darkness, the cold stone floor beneath you.",
                "As your eyes adjust, ancient walls stretch endlessly in all directions.",
                "",
                "This is Daedalus' " + ConsoleColor.BRIGHT_RED + "Infinite Dungeon" + ConsoleColor.RESET
                        + " - a dungeon that shifts and changes,",
                "consuming all who dare enter its depths.",
                "",
                "Legends speak of an exit hidden deep within, but few have ever found it.",
                "Those who tried met their fate at the hands of " + ConsoleColor.RED + "monsters" + ConsoleColor.RESET
                        + ", " + ConsoleColor.YELLOW + "traps" + ConsoleColor.RESET + ",",
                "and the labyrinth's own " + ConsoleColor.MAGENTA + "twisted magic" + ConsoleColor.RESET + ".",
                "",
                ConsoleColor.BRIGHT_GREEN + player.getName() + ConsoleColor.RESET + ", your journey will be perilous.",
                "You must descend through " + ConsoleColor.BRIGHT_CYAN + "countless floors" + ConsoleColor.RESET
                        + ", battling fierce creatures,",
                "solving ancient puzzles, and gathering powerful equipment.",
                "",
                "The dungeon's exit lies on " + ConsoleColor.BRIGHT_YELLOW + "Floor "
                        + settings.difficulty.firstExitFloor + ConsoleColor.RESET + " and beyond.",
                "Only by reaching that depth can you escape this cursed place.",
                "",
                ConsoleColor.BRIGHT_RED + "⚠" + ConsoleColor.RESET + " But beware - every "
                        + ConsoleColor.BRIGHT_MAGENTA + "5th floor" + ConsoleColor.RESET + " harbors a "
                        + ConsoleColor.BRIGHT_RED + "powerful boss" + ConsoleColor.RESET + ".",
                "These guardians will test your strength and determination.",
                "",
                "",
                ConsoleColor.BRIGHT_CYAN + "Will you find the exit and escape... or become another lost soul?"
                        + ConsoleColor.RESET
        };

        for (String line : introLines) {
            view.printLine("  " + line);
            try {
                Thread.sleep(70); // Pause between lines for dramatic effect
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        view.printLine("");
        view.printLine("");
        view.printCentered(ConsoleColor.BRIGHT_CYAN + "═══════════════════════════════════════════════════════"
                + ConsoleColor.RESET);
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_YELLOW + "Your adventure begins on Floor 1..." + ConsoleColor.RESET);
        view.printLine("");
        view.pressAnyKey();

        logger.info("CUTSCENE", "Intro cutscene completed for player: " + player.getName());
    }
}