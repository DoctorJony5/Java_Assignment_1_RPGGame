// Jonathan Decondé - 3196362
package game.systems;

import game.entities.Player;
import game.systems.dungeons.DungeonFloor;
import game.systems.dungeons.Room;
import game.ui.View;
import util.ConsoleColor;
import util.Logger;

// The debug system was a series of tools I designed to allow me to test specific things, rush through the game and whatnot.
// There should be no real reason to use this as a player.
public class DebugCommandSystem {
    private Player player;
    private View view;
    private Logger logger;
    private boolean enabled;
    private DungeonFloor currentFloor; // Reference to current floor for room info

    public DebugCommandSystem(Player player, View view, boolean enabled) {
        this.player = player;
        this.view = view;
        this.logger = Logger.getInstance();
        this.enabled = enabled;
        this.currentFloor = null;
    }

    public void setCurrentFloor(DungeonFloor floor) {
        this.currentFloor = floor;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.debug("DEBUG", "Debug commands " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Check that debug is enabled and request not empty.
    public boolean isDebugCommand(String input) {
        return enabled && input != null;
    }

    // Return true if the command is real / true
    public boolean executeCommand(String input) {
        if (!enabled) {
            view.showError("Debug mode is not enabled!");
            return false;
        }

        String command = input.substring(1).trim().toLowerCase();
        String[] parts = command.split("\\s+");
        String cmd = parts[0];

        logger.debug("DEBUG", "Executing debug command: " + command);

        try {
            switch (cmd) {
                case "help":
                    showHelp();
                    return true;

                case "heal":
                    healPlayer(parts.length > 1 ? Integer.parseInt(parts[1]) : player.getMaxHealth());
                    return true;

                case "stamina":
                    restoreStamina(parts.length > 1 ? Integer.parseInt(parts[1]) : player.getMaxStamina());
                    return true;

                case "giveitem":
                    if (parts.length < 2) {
                        view.showError("Usage: /giveitem <type> [rarity]");
                        return false;
                    }
                    giveItem(parts[1], parts.length > 2 ? parts[2] : "common");
                    return true;

                case "level":
                    if (parts.length < 2) {
                        view.showError("Usage: /level <amount>");
                        return false;
                    }
                    levelUp(Integer.parseInt(parts[1]));
                    return true;

                case "gold":
                    if (parts.length < 2) {
                        view.showError("Usage: /gold <amount>");
                        return false;
                    }
                    giveGold(Integer.parseInt(parts[1]));
                    return true;

                case "tp":
                case "teleport":
                    if (parts.length < 3) {
                        view.showError("Usage: /tp <x> <y>");
                        return false;
                    }
                    teleport(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    return true;

                case "god":
                case "godmode":
                    toggleGodMode(); // tends not to work because this is poorly designed & implemented; though it's
                                     // hard to die.
                    return true;

                case "reveal":
                    revealMap();
                    return true;

                case "stats":
                    showStats();
                    return true;

                case "roomtype":
                    showRoomType();
                    return true;

                case "kill":
                    killAllMonsters();
                    return true;

                default:
                    view.showError("Unknown debug command: /" + cmd);
                    view.showInfo("Type /help for a list of commands");
                    return false;
            }
        } catch (NumberFormatException e) {
            view.showError("Invalid number format");
            logger.warn("DEBUG", "Invalid number in command: " + command);
            return false;
        } catch (Exception e) {
            view.showError("Error executing command: " + e.getMessage());
            logger.error("DEBUG", "Command execution failed", e);
            return false;
        }
    }

    // Commands, menus, etc.
    // Code is self explanatory, each method handles a specific command, most are
    // quite simple really

    private void showHelp() {
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "╔═══════════════════════════════════════════════════╗"
                + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "║" + ConsoleColor.BOLD +
                "              DEBUG COMMANDS                      " +
                ConsoleColor.RESET + ConsoleColor.BRIGHT_MAGENTA + "║" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "╠═══════════════════════════════════════════════════╣"
                + ConsoleColor.RESET);

        String[][] commands = {
                { "/help", "Show this help message" },
                { "/heal [amount]", "Heal player (default: full)" },
                { "/stamina [amount]", "Restore stamina (default: full)" },
                { "/level <amount>", "Gain levels" },
                { "/roomtype", "Show current room type" },
                { "/stats", "Show detailed player stats" }
        };

        for (String[] cmd : commands) {
            String line = String.format("║ %-20s - %-26s║", cmd[0], cmd[1]);
            view.printLine(ConsoleColor.BRIGHT_MAGENTA + line + ConsoleColor.RESET);
        }

        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "╚═══════════════════════════════════════════════════╝"
                + ConsoleColor.RESET);
        view.printLine("");
    }

    private void healPlayer(int amount) {
        int oldHealth = player.getHealth();
        player.heal(amount);
        int healed = player.getHealth() - oldHealth;
        view.showSuccess("Healed " + healed + " HP (now at " + player.getHealth() + "/" + player.getMaxHealth() + ")");
        logger.debug("DEBUG", "Player healed: " + healed + " HP");
    }

    private void restoreStamina(int amount) {
        int oldStamina = player.getStamina();
        player.restoreStamina(amount);
        int restored = player.getStamina() - oldStamina;
        view.showSuccess("Restored " + restored + " stamina (now at " + player.getStamina() + "/"
                + player.getMaxStamina() + ")");
        logger.debug("DEBUG", "Player stamina restored: " + restored);
    }

    private void giveItem(String type, String rarityStr) {
        // Also not working rn
        view.showWarning("Give item command removed - sorry for the inconvenience");
        view.showInfo("Requested: " + type + " (" + rarityStr + ")");
        logger.debug("DEBUG", "Item request: " + type + " " + rarityStr);
    }

    private void levelUp(int levels) {
        int oldLevel = player.getLevel();
        for (int i = 0; i < levels; i++) { // Easy way to add levels; although you could just do level + newLevel =
                                           // level, that caused issues with a earlier implementation of levels.
            player.gainExperience(player.getExperienceToNextLevel());
        }
        view.showSuccess("Gained " + levels + " levels! (Lv " + oldLevel + " -> Lv " + player.getLevel() + ")");
        logger.debug("DEBUG", "Player leveled up: +" + levels + " levels");
    }

    private void giveGold(int amount) {
        view.showWarning("Gold system not yet implemented");
        view.showInfo("Would give: " + amount + " gold");
        logger.debug("DEBUG", "Gold request: " + amount);
    }

    private void teleport(int x, int y) {
        view.showWarning("Teleport command removed - sorry for the inconvenience");
        view.showInfo("Requested teleport to: (" + x + ", " + y + ")");
        logger.debug("DEBUG", "Teleport requested: (" + x + ", " + y + ")");
        // DO NOT USE
        // NOT IMPLEMENTED ANYMORE - CHANGES DONE
    }

    private void toggleGodMode() {
        // No longer works
        view.showWarning("God mode command removed - sorry for the inconvenience");
        logger.debug("DEBUG", "God mode toggle requested");
    }

    private void revealMap() {
        // No longer works
        view.showWarning("Map reveal command removed - sorry for the inconvenience");
        logger.debug("DEBUG", "Map reveal requested");
    }

    private void showStats() {
        view.printLine("");
        view.printLine("═══ DETAILED STATS ═══");
        view.printLine("Name: " + player.getName());
        view.printLine("Level: " + player.getLevel());
        view.printLine("HP: " + player.getHealth() + "/" + player.getMaxHealth());
        view.printLine("Stamina: " + player.getStamina() + "/" + player.getMaxStamina());
        view.printLine("Strength: " + player.getStrength());
        view.printLine("Resilience: " + player.getResilience());
        view.printLine("Speed: " + player.getSpeed());
        view.printLine("Intelligence: " + player.getIntelligence());
        view.printLine("Luck: " + player.getLuck());
        view.printLine("Experience: " + player.getExperience() + "/" + player.getExperienceToNextLevel());
        view.printLine("Floor Reached: " + player.getFloorReached());
        view.printLine("Total Score: " + player.getTotalScore());
        view.printLine("═════════════════════");
        view.printLine("");
    } // May not work fully ?

    private void killAllMonsters() {
        view.showWarning("Kill all monsters command removed - sorry for the inconvenience");
        logger.debug("DEBUG", "Kill all monsters requested");
    } // Changed how the dungeon works so this doesn't work.

    private void showRoomType() {
        if (currentFloor == null) {
            view.showWarning("No active floor!");
            return;
        }

        int playerX = currentFloor.getPlayerX();
        int playerY = currentFloor.getPlayerY();
        Room room = currentFloor.getRoomAt(playerX, playerY);

        if (room == null) {
            view.showInfo("Current room: NONE (in corridor/empty space)");
            return;
        }

        String roomInfo = String.format("Current room: %s (Elite: %s, Explored: %s, Completed: %s, Monsters: %d)",
                room.getType(),
                room.isElite(),
                room.isExplored(),
                room.isCompleted(),
                room.getMonsters().size());

        view.printLine("");
        view.showSuccess(roomInfo);
        view.printLine("");
    }
}
