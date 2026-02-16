// Jonathan Decondé - 3196362
package game.ui;

import game.entities.Player;
import game.entities.monsters.Monster;
import game.systems.combat.Ability;
import util.ConsoleColor;
import util.DynArray;

// Combat View handles all the combat viewing stuff
//
public class CombatView {
    private final View baseView;

    public CombatView(View baseView) {
        this.baseView = baseView;
    }

    // Compatibility wrappers for existing View/CombatSystem calls
    public void printCombatHeader(Player player, Monster monster) {
        showCombatHeader(player, monster);
    }

    public void showCombatState(Player player, Monster monster, boolean isPlayerTurn, String lastAction) {
        // isPlayerTurn currently no longer used for rendering
        showCombatStatus(player, monster, lastAction, 0, false, 0, 0, 0, 0);
    }

    public void showCombatState(Player player, Monster monster, boolean isPlayerTurn, String lastAction,
            int comboCount, boolean playerFocused, int playerBleed, int playerPoison,
            int monsterBleed, int monsterPoison) {
        showCombatStatus(player, monster, lastAction, comboCount, playerFocused,
                playerBleed, playerPoison, monsterBleed, monsterPoison);
    }

    // Combat stuff
    public void showCombatHeader(Player player, Monster monster) {
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "╔═══════════════════════════════════════════════════════════════════════════╗" +
                ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "║                            ⚔  COMBAT START  ⚔                            ║" +
                ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "╚═══════════════════════════════════════════════════════════════════════════╝" +
                ConsoleColor.RESET);
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_YELLOW + player.getName() + ConsoleColor.RESET +
                " encounters " + ConsoleColor.BRIGHT_RED + monster.getName() + ConsoleColor.RESET + "!");
        baseView.printLine("");
    } // nice looking menu / box, alert / indicator.

    // Status Board
    public void showCombatStatus(Player player, Monster monster, String lastAction,
            int comboCount, boolean playerFocused, int playerBleed, int playerPoison,
            int monsterBleed, int monsterPoison) {
        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╔═══════════════════════════════════════════════════════════════════════════╗" +
                ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "║                              COMBAT STATUS                                ║" +
                ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╠═══════════════════════════════════════════════════════════════════════════╣" +
                ConsoleColor.RESET);

        // Player section
        showCombatantHeader(player.getName(), player.getLevel());
        showHealthBar(player.getHealth(), player.getMaxHealth(), ConsoleColor.BRIGHT_GREEN); // get the player health
                                                                                             // and display it
        showStaminaBar(player.getStamina(), player.getMaxStamina(), ConsoleColor.BRIGHT_BLUE); // get the player stamina
                                                                                               // and display it

        // Show player buffs and debuffs
        showPlayerStatus(comboCount, playerFocused, playerBleed, playerPoison, player);

        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║" + " ".repeat(76) + "║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╠═══════════════════════════════════════════════════════════════════════════╣" +
                ConsoleColor.RESET);

        // Monster section
        showCombatantHeader(monster.getName(), monster.getLevel());
        showHealthBar(monster.getHealth(), monster.getMaxHealth(), ConsoleColor.BRIGHT_RED); // get and display health
                                                                                             // data
        showStaminaBar(monster.getStamina(), monster.getMaxStamina(), ConsoleColor.BRIGHT_MAGENTA); // Get and display
                                                                                                    // stamina data

        // Show monster debuffs and elemental weakness
        showMonsterStatus(monsterBleed, monsterPoison, monster);

        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╚═══════════════════════════════════════════════════════════════════════════╝" +
                ConsoleColor.RESET);

        if (!lastAction.isEmpty()) {
            baseView.printLine("");
            baseView.printLine(lastAction);
        }
    }

    private void showCombatantHeader(String name, int level) {
        String header = String.format("║ %-20s  Lv %d", name, level);
        int padding = 50 - header.length() + 5;
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + header + " ".repeat(padding) + "║" + ConsoleColor.RESET);
    }

    private void showHealthBar(int current, int max, String color) {
        String bar = formatBar("HP", current, max, 40, color);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " + bar + "              ║" + ConsoleColor.RESET);
    } // System for HealthBar

    private void showStaminaBar(int current, int max, String color) {
        String bar = formatBar("ST", current, max, 40, color);
        baseView.printLine(ConsoleColor.BRIGHT_CYAN + "║ " + bar + "              ║" + ConsoleColor.RESET);
    } // System for Staminabar

    private void showPlayerStatus(int comboCount, boolean focused, int bleed, int poison, Player player) {
        StringBuilder status = new StringBuilder("║ ");

        // Show combo counter
        if (comboCount > 0) {
            status.append(ConsoleColor.BRIGHT_YELLOW)
                    .append("COMBO x").append(comboCount)
                    .append(" (+").append(comboCount * 2).append(" DMG)")
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Show focus buff
        if (focused) {
            status.append(ConsoleColor.BRIGHT_CYAN)
                    .append("✦FOCUSED (+35%)")
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Show debuffs
        if (bleed > 0) {
            status.append(ConsoleColor.BRIGHT_RED)
                    .append("BLEEDING(").append(bleed).append(")")
                    .append(ConsoleColor.RESET).append("  ");
        }
        if (poison > 0) {
            status.append(ConsoleColor.BRIGHT_GREEN)
                    .append("☠POISONED(").append(poison).append(")")
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Show elemental affinity if set
        if (player.getElementalAffinity() != null) {
            status.append(ConsoleColor.BRIGHT_MAGENTA)
                    .append("◆").append(player.getElementalAffinity().name())
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Pad to line width
        String statusStr = status.toString();
        int visibleLength = statusStr.replaceAll("\\u001B\\[[;\\d]*m", "").length();
        int padding = 76 - visibleLength;
        if (padding > 0) {
            status.append(" ".repeat(padding));
        }
        status.append("║");

        baseView.printLine(status.toString());
    }

    private void showMonsterStatus(int bleed, int poison, Monster monster) {
        StringBuilder status = new StringBuilder("║ ");

        // Show debuffs
        if (bleed > 0) {
            status.append(ConsoleColor.BRIGHT_RED)
                    .append("🩸BLEEDING(").append(bleed).append(")")
                    .append(ConsoleColor.RESET).append("  ");
        }
        if (poison > 0) {
            status.append(ConsoleColor.BRIGHT_GREEN)
                    .append("☠POISONED(").append(poison).append(")")
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Show elemental weakness
        if (monster.getWeakness() != null) {
            status.append(ConsoleColor.BRIGHT_YELLOW)
                    .append("⚠WEAK TO: ").append(monster.getWeakness().name())
                    .append(ConsoleColor.RESET).append("  ");
        }

        // Pad to line width
        String statusStr = status.toString();
        int visibleLength = statusStr.replaceAll("\\u001B\\[[;\\d]*m", "").length();
        int padding = 76 - visibleLength;
        if (padding > 0) {
            status.append(" ".repeat(padding));
        }
        status.append("║");

        baseView.printLine(status.toString());
    }

    // Formatting for bar.
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

    // Ability menu - no longer used.
    public void showAbilityMenu(DynArray<Ability> abilities, Player player) {
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╔══ YOUR TURN ═══════════════════════════════════════════════════╗" +
                ConsoleColor.RESET);

        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            String abilityLine = formatAbilityOption(i + 1, ability, player);
            baseView.printLine(ConsoleColor.BRIGHT_CYAN + abilityLine + ConsoleColor.RESET);
        }

        baseView.printLine(ConsoleColor.BRIGHT_CYAN +
                "╚═══════════════════════════════════════════════════════════════╝" +
                ConsoleColor.RESET);
        baseView.print(ConsoleColor.BRIGHT_YELLOW + "Choose ability" + ConsoleColor.RESET + " > ");
    }

    private String formatAbilityOption(int number, Ability ability, Player player) {
        if (ability.isDefensive()) {
            return String.format("║ [%d] %-18s %-12s Cost: %2d SP              ║",
                    number, ability.getName(), "(Utility)", ability.getStaminaCost());
        } else {
            int[] range = estimateDamageRange(ability, player);
            return String.format("║ [%d] %-18s %-12s Cost: %2d SP              ║",
                    number, ability.getName(),
                    String.format("(%d-%d dmg)", range[0], range[1]),
                    ability.getStaminaCost());
        }
    }

    private int[] estimateDamageRange(Ability ability, Player player) {
        int weaponDamage = 0;
        if (player.getEquippedWeapon() != null) {
            weaponDamage = player.getEquippedWeapon().getDamage();
        }

        // This game uses a Diablo inspired system for damage and etc
        // Weapons have a static damage thing
        // Players have a strength attribute
        // Therefore we can get a max and minimum damage for each "ability".
        // This isn't really ideal to be honest, but oh well.

        int baseMin = ability.getMinDamage() + player.getStrength() + weaponDamage;
        int baseMax = ability.getMaxDamage() + player.getStrength() + weaponDamage;

        // Stub for a cooler system I had in mind.
        // "power" abilities are just stronger.
        if (ability.getName().toLowerCase().contains("power")) {
            baseMin = (int) Math.max(1, baseMin * 0.8);
            baseMax = (int) (baseMax * 1.2);
        }

        return new int[] { Math.max(1, baseMin), Math.max(1, baseMax) };
    }

    // In combat messages
    public void showAction(String message, ActionType type) {
        String icon = getActionIcon(type);
        String color = getActionColor(type);
        baseView.printColoredLine(icon + " " + message, color);
    }

    public void showCriticalHit() {
        baseView.printLine(ConsoleColor.BRIGHT_YELLOW + "✦ CRITICAL HIT! ✦" + ConsoleColor.RESET);
    }

    public void showComboMessage(int comboCount) {
        if (comboCount > 1) {
            baseView.printLine(ConsoleColor.BRIGHT_MAGENTA +
                    "⚡ " + comboCount + "x COMBO! ⚡" + ConsoleColor.RESET);
        }
    }

    public void showStatusEffect(String effect, int turnsRemaining) {
        String color = effect.toLowerCase().contains("bleed") ? ConsoleColor.BRIGHT_RED : ConsoleColor.BRIGHT_GREEN;
        baseView.printColoredLine("⚠ " + effect + " (" + turnsRemaining + " turns remaining)", color);
    }

    public void showVictory(String monsterName, int expGained) {
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_GREEN +
                "╔═════════════════════════════════════════╗" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_GREEN +
                "║          ✓ VICTORY ✓                   ║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_GREEN +
                "╚═════════════════════════════════════════╝" + ConsoleColor.RESET);
        baseView.printLine("");
        baseView.printLine("You have defeated the " + ConsoleColor.BRIGHT_RED +
                monsterName + ConsoleColor.RESET + "!");
        baseView.printLine(ConsoleColor.BRIGHT_YELLOW + "+" + expGained + " EXP" + ConsoleColor.RESET);
    }

    public void showDefeat() {
        baseView.printLine("");
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "╔═════════════════════════════════════════╗" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "║          ✗ DEFEAT ✗                    ║" + ConsoleColor.RESET);
        baseView.printLine(ConsoleColor.BRIGHT_RED +
                "╚═════════════════════════════════════════╝" + ConsoleColor.RESET);
        baseView.printLine("");
        baseView.printLine("You have been defeated...");
    }

    // Helper stuff
    private String getActionIcon(ActionType type) {
        return switch (type) {
            case OFFENSIVE -> "⚔";
            case DEFENSIVE -> "🛡";
            case UTILITY -> "✦";
            case DAMAGE_TAKEN -> "✗";
            case HEALING -> "✚";
        }; // found online
    }

    private String getActionColor(ActionType type) {
        return switch (type) {
            case OFFENSIVE -> ConsoleColor.BRIGHT_RED;
            case DEFENSIVE -> ConsoleColor.BRIGHT_BLUE;
            case UTILITY -> ConsoleColor.BRIGHT_YELLOW;
            case DAMAGE_TAKEN -> ConsoleColor.RED;
            case HEALING -> ConsoleColor.BRIGHT_GREEN;
        };
    } // Not super happy with this I must admit

    // Action type enum.
    public enum ActionType {
        OFFENSIVE,
        DEFENSIVE,
        UTILITY,
        DAMAGE_TAKEN,
        HEALING
    }
}
