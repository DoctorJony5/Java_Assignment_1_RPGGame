// Jonathan Decondé - 3196362
package game.systems;

import game.entities.Player;
import game.items.LoreItem;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.DynArray;
import util.Logger;

public class SaveSystem {
    private static final String SAVE_DIR = "saves";
    private static final String SAVE_EXTENSION = ".sav";
    private static final String SAVE_VERSION = "2";
    private Logger logger;

    public SaveSystem() {
        this.logger = Logger.getInstance();
        createSaveDirectory();
    }

    private void createSaveDirectory() {
        try {
            Path savePath = Paths.get(SAVE_DIR);
            if (!Files.exists(savePath)) {
                Files.createDirectory(savePath);
                logger.debug("SAVE", "Created save directory");
            }
        } catch (IOException e) {
            logger.error("SAVE", "Failed to create save directory", e);
        }
    } // Create a directory to save stuff.

    public boolean saveGame(Player player, int currentFloor, long runSeed) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = player.getName() + "_" + timestamp + SAVE_EXTENSION;
        String filepath = SAVE_DIR + File.separator + filename;

        logger.info("SAVE", "Attempting to save game to: " + filepath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // Write to a new file, which is just player name and time stamp .sav (basic,
            // easy)

            // Header
            writer.write("# JavaRPG Save File\n");
            writer.write("# Version: " + SAVE_VERSION + "\n");
            writer.write("# Created: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("# Player: " + player.getName() + "\n");
            writer.write("# Floor: " + currentFloor + "\n");
            writer.write("\n");

            // Basic Info
            writer.write("[PLAYER]\n");
            writer.write("version=" + SAVE_VERSION + "\n");
            writer.write("name=" + player.getName() + "\n");
            writer.write("level=" + player.getLevel() + "\n");
            writer.write("experience=" + player.getExperience() + "\n");
            writer.write("health=" + player.getHealth() + "\n");
            writer.write("maxHealth=" + player.getMaxHealth() + "\n");
            writer.write("stamina=" + player.getStamina() + "\n");
            writer.write("maxStamina=" + player.getMaxStamina() + "\n");
            writer.write("strength=" + player.getStrength() + "\n");
            writer.write("resilience=" + player.getResilience() + "\n");
            writer.write("speed=" + player.getSpeed() + "\n");
            writer.write("intelligence=" + player.getIntelligence() + "\n");
            writer.write("luck=" + player.getLuck() + "\n");
            writer.write("unspentStatPoints=" + player.getUnspentStatPoints() + "\n");
            writer.write("floorReached=" + player.getFloorReached() + "\n");
            writer.write("totalScore=" + player.getTotalScore() + "\n");
            writer.write("currentFloor=" + currentFloor + "\n");
            writer.write("runSeed=" + runSeed + "\n");
            writer.write("\n");

            // Statistics
            writer.write("[STATISTICS]\n");
            writer.write("monstersDefeated=" + player.getMonstersDefeated() + "\n");
            writer.write("puzzlesSolved=" + player.getPuzzlesSolved() + "\n");
            writer.write("mimicsDefeated=" + player.getMimicsDefeated() + "\n");
            writer.write("itemsCrafted=" + player.getItemsCrafted() + "\n");
            writer.write("chestsOpened=" + player.getChestsOpened() + "\n");
            writer.write("\n");

            // A nice oversight on my end is that, because all weapons bar the DEBUG ones
            // are Dynamic,
            // this is completely useless !
            // All that this saves are names, and nothing else!
            // This is unfortunate and I don't have the time to rework or fix this
            // So this is all kinda useless

            // Equipment - NOW SAVES FULL ITEM DATA
            writer.write("[EQUIPMENT]\n");
            if (player.getEquippedWeapon() != null) {
                writer.write("weapon=" + player.getEquippedWeapon().serialize() + "\n");
            }
            if (player.getEquippedHelmet() != null) {
                writer.write("helmet=" + player.getEquippedHelmet().serialize() + "\n");
            }
            if (player.getEquippedChestplate() != null) {
                writer.write("chestplate=" + player.getEquippedChestplate().serialize() + "\n");
            }
            if (player.getEquippedLeggings() != null) {
                writer.write("leggings=" + player.getEquippedLeggings().serialize() + "\n");
            }
            if (player.getEquippedBoots() != null) {
                writer.write("boots=" + player.getEquippedBoots().serialize() + "\n");
            }
            writer.write("\n");

            // Inventory - NOW SAVES FULL ITEM DATA
            writer.write("[INVENTORY]\n");
            writer.write("count=" + player.getInventory().size() + "\n");
            for (int i = 0; i < player.getInventory().size(); i++) {
                writer.write("item" + i + "=" + player.getInventory().get(i).serialize() + "\n");
            }
            writer.write("\n");

            // Lore Collection
            writer.write("[LORE]\n");
            writer.write("count=" + player.getLoreCollection().size() + "\n");
            for (int i = 0; i < player.getLoreCollection().size(); i++) {
                LoreItem lore = player.getLoreCollection().get(i);
                writer.write("lore" + i + "=" + lore.getName() + "|" + lore.getFloorFound() + "\n");
            }
            writer.write("\n");

            int checksum = computeChecksum(player, currentFloor, runSeed);
            writer.write("[META]\n");
            writer.write("checksum=" + checksum + "\n");
            writer.write("version=" + SAVE_VERSION + "\n");
            writer.write("\n");

            writer.write("# End of save file\n");

            logger.info("SAVE", "Game saved successfully to: " + filename);
            return true;

        } catch (IOException e) {
            logger.error("SAVE", "Failed to save game", e);
            return false;
            // no idea what could cause this to happen
        }
    }

    public DynArray<String> listSaveFiles() {
        DynArray<String> saveFiles = new DynArray<>();

        try {
            Path savePath = Paths.get(SAVE_DIR);
            if (Files.exists(savePath) && Files.isDirectory(savePath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(savePath, "*" + SAVE_EXTENSION)) {
                    for (Path entry : stream) {
                        saveFiles.add(entry.getFileName().toString());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("SAVE", "Failed to list save files", e);
        }
        // very simply to just list the saves.
        return saveFiles;
    }

    public SaveData loadGame(String filename) {
        String filepath = SAVE_DIR + File.separator + filename;
        logger.info("SAVE", "Attempting to load game from: " + filepath);

        SaveData data = new SaveData();

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            String currentSection = "";
            boolean sawPlayerSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                // Check for section headers
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    if ("PLAYER".equals(currentSection)) {
                        sawPlayerSection = true;
                    }
                    continue;
                }

                // Parse key=value pairs
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts.length > 1 ? parts[1].trim() : "";

                    switch (currentSection) {
                        case "PLAYER" -> parsePlayerData(data, key, value);
                        case "STATISTICS" -> parseStatistics(data, key, value);
                        case "EQUIPMENT" -> parseEquipment(data, key, value);
                        case "INVENTORY" -> parseInventory(data, key, value);
                        case "LORE" -> parseLore(data, key, value);
                        case "META" -> parseMeta(data, key, value);
                    }
                }
            }

            if (!sawPlayerSection) {
                logger.error("SAVE", "Missing [PLAYER] section; save file is invalid");
                return null;
            }

            int calculatedChecksum = computeChecksum(data);
            if (data.checksum != 0 && calculatedChecksum != data.checksum) {
                logger.error("SAVE", "Checksum mismatch. Expected " + data.checksum + " got " + calculatedChecksum);
                return null;
            }

            if (!SAVE_VERSION.equals(data.version) && !data.version.isEmpty()) {
                logger.warn("SAVE", "Loading save with version " + data.version + " (current " + SAVE_VERSION + ")");
            }

            logger.info("SAVE", "Game loaded successfully from: " + filename);
            return data;

        } catch (IOException e) {
            logger.error("SAVE", "Failed to load game", e);
            return null;
        }
    }

    private void parsePlayerData(SaveData data, String key, String value) {
        try {
            switch (key) {
                case "name" -> data.playerName = value;
                case "level" -> data.level = Integer.parseInt(value);
                case "experience" -> data.experience = Integer.parseInt(value);
                case "health" -> data.health = Integer.parseInt(value);
                case "maxHealth" -> data.maxHealth = Integer.parseInt(value);
                case "stamina" -> data.stamina = Integer.parseInt(value);
                case "maxStamina" -> data.maxStamina = Integer.parseInt(value);
                case "strength" -> data.strength = Integer.parseInt(value);
                case "resilience" -> data.resilience = Integer.parseInt(value);
                case "speed" -> data.speed = Integer.parseInt(value);
                case "intelligence" -> data.intelligence = Integer.parseInt(value);
                case "luck" -> data.luck = Integer.parseInt(value);
                case "unspentStatPoints" -> data.unspentStatPoints = Integer.parseInt(value);
                case "floorReached" -> data.floorReached = Integer.parseInt(value);
                case "totalScore" -> data.totalScore = Long.parseLong(value);
                case "currentFloor" -> data.currentFloor = Integer.parseInt(value);
                case "runSeed" -> data.runSeed = Long.parseLong(value);
                case "version" -> data.version = value;
            }
        } catch (NumberFormatException e) {
            logger.warn("SAVE", "Failed to parse player data: " + key + "=" + value);
        }
    }

    private void parseStatistics(SaveData data, String key, String value) {
        try {
            switch (key) {
                case "monstersDefeated" -> data.monstersDefeated = Integer.parseInt(value);
                case "puzzlesSolved" -> data.puzzlesSolved = Integer.parseInt(value);
                case "mimicsDefeated" -> data.mimicsDefeated = Integer.parseInt(value);
                case "itemsCrafted" -> data.itemsCrafted = Integer.parseInt(value);
                case "chestsOpened" -> data.chestsOpened = Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            logger.warn("SAVE", "Failed to parse statistics: " + key + "=" + value);
        }
    }

    private void parseEquipment(SaveData data, String key, String value) {
        switch (key) {
            case "weapon" -> data.equippedWeapon = value;
            case "helmet" -> data.equippedHelmet = value;
            case "chestplate" -> data.equippedChestplate = value;
            case "gauntlets" -> data.equippedGauntlets = value;
            case "leggings" -> data.equippedLeggings = value;
            case "boots" -> data.equippedBoots = value;
        }
    }

    private void parseInventory(SaveData data, String key, String value) {
        if (key.startsWith("item")) {
            data.inventoryItems.add(value);
        }
    }

    private void parseLore(SaveData data, String key, String value) {
        if (key.startsWith("lore")) {
            data.loreItems.add(value);
        }
    }

    private void parseMeta(SaveData data, String key, String value) {
        switch (key) {
            case "checksum" -> {
                try {
                    data.checksum = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    logger.warn("SAVE", "Invalid checksum format: " + value);
                }
            }
            case "version" -> data.version = value;
        }
    }

    private int computeChecksum(Player player, int currentFloor, long runSeed) {
        int result = 17;
        String name = player.getName() == null ? "" : player.getName();
        result = 31 * result + name.hashCode();
        result = 31 * result + player.getLevel();
        result = 31 * result + player.getExperience();
        result = 31 * result + player.getHealth();
        result = 31 * result + player.getMaxHealth();
        result = 31 * result + player.getStamina();
        result = 31 * result + player.getMaxStamina();
        result = 31 * result + player.getStrength();
        result = 31 * result + player.getResilience();
        result = 31 * result + player.getSpeed();
        result = 31 * result + player.getIntelligence();
        result = 31 * result + player.getLuck();
        result = 31 * result + player.getUnspentStatPoints();
        result = 31 * result + player.getFloorReached();
        result = 31 * result + currentFloor;
        result = 31 * result + (int) (runSeed ^ (runSeed >>> 32));
        return result;
    }

    private int computeChecksum(SaveData data) {
        int result = 17;
        String name = data.playerName == null ? "" : data.playerName;
        result = 31 * result + name.hashCode();
        result = 31 * result + data.level;
        result = 31 * result + data.experience;
        result = 31 * result + data.health;
        result = 31 * result + data.maxHealth;
        result = 31 * result + data.stamina;
        result = 31 * result + data.maxStamina;
        result = 31 * result + data.strength;
        result = 31 * result + data.resilience;
        result = 31 * result + data.speed;
        result = 31 * result + data.intelligence;
        result = 31 * result + data.luck;
        result = 31 * result + data.unspentStatPoints;
        result = 31 * result + data.floorReached;
        result = 31 * result + data.currentFloor;
        result = 31 * result + (int) (data.runSeed ^ (data.runSeed >>> 32));
        return result;
    }

    public static class SaveData {

        // this is nice to look at though.
        // These are basic

        public String playerName = "";
        public int level = 1;
        public int experience = 0;
        public int health = 100;
        public int maxHealth = 100;
        public int stamina = 100;
        public int maxStamina = 100;
        public int strength = 10;
        public int resilience = 5;
        public int speed = 10;
        public int intelligence = 10;
        public int luck = 5;
        public int unspentStatPoints = 0;
        public int floorReached = 1;
        public long totalScore = 0;
        public int currentFloor = 1;
        public long runSeed = 0L;

        public int monstersDefeated = 0;
        public int puzzlesSolved = 0;
        public int mimicsDefeated = 0;
        public int itemsCrafted = 0;
        public int chestsOpened = 0;

        public String equippedWeapon = null;
        public String equippedHelmet = null;
        public String equippedChestplate = null;
        public String equippedGauntlets = null;
        public String equippedLeggings = null;
        public String equippedBoots = null;

        public DynArray<String> inventoryItems = new DynArray<>();
        public DynArray<String> loreItems = new DynArray<>();

        public int checksum = 0;
        public String version = "";
    }
}
