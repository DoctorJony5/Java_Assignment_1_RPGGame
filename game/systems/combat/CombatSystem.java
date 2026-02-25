// Jonathan Decondé - 3196362
package game.systems.combat;

import game.ItemType;
import game.entities.Companion;
import game.entities.Player;
import game.entities.monsters.Monster;
import game.entities.monsters.MonsterAbility;
import game.items.Item;
import game.ui.View;
import util.ConsoleColor;
import util.DynArray;
import util.Logger;
import util.RNG;

// The infamous combat system!
// This handles turn-based combat between the player and monsters.
// Combat flow: initialization -> first strike bonus -> alternating turns -> end combat
//
// THe combat system is designed to be engaging and rewarding
// But it's also the most complicated system to get "right" - what makes combat fun ? 

// As always, lots of these things could be FINAL, but I'm still changing some small things
// The code works, so I'm leaving it as is for now.
public class CombatSystem {
    private Player player; // The player character
    private Monster monster; // The enemy being fought (this is inelegant, but works for now)
    private View view; // UI for displaying combat
    private RNG rng; // Random number generator for attack calculations - could be improved,
                     // randomness is the bane of "fun" and good game design.
    private Logger logger;

    private DynArray<Ability> playerAbilities; // List of moves the player can perform
    private DynArray<Companion> partyMembers;

    // Status effects persist for a number of turns and deal damage each turn
    private int playerBleedTurns; // Bleeding: takes damage each turn (from weapon trait)
    private int playerPoisonTurns; // Poison: takes damage each turn (from weapon trait)
    private int monsterBleedTurns; // Monster version of bleed
    private int monsterPoisonTurns; // Monster version of poison
    private int playerParryBlock; // Parry ability effect: blocks the next attack and counters

    private static final int BLEED_DAMAGE = 5; // Damage per turn from bleeding
    private static final int POISON_DAMAGE = 3; // Damage per turn from poison
    private static final double FIRST_STRIKE_DAMAGE_BONUS = 0.15; // 15% damage bonus for first strike

    private int comboCount; // Current combo streak (increases damage on next hit)
    private int combatHealingCap; // Maximum health that can be restored via potions during combat
    // (Prevents healing to full health, forcing player to use strategy)
    // FLEE SYSTEM DISABLED
    // private boolean playerFled; // Whether player attempted to flee this combat
    // private boolean monsterFled; // Whether monster attempted to flee this combat
    private boolean playerFocused; // Focus buff: next attack deals 50% more damage (poorly implemented, more of a
                                   // proof of concept.)
    private boolean playerHasFirstStrikeBonus = false; // Player's first attack gets +15% damage

    public CombatSystem(Player player, Monster monster, View view) { // Constructor
        this.player = player;
        this.monster = monster;
        this.view = view;
        this.rng = new RNG();
        this.logger = Logger.getInstance();
        this.comboCount = 0;
        this.partyMembers = player.getCompanions();
        // Prevention of potion abuse. Reduced from 0.75 to 0.60 for harder combat
        this.combatHealingCap = (int) (player.getMaxHealth() * 0.60);
        // FLEE SYSTEM DISABLED
        // this.playerFled = false;
        // this.monsterFled = false;
        initializePlayerAbilities();
        logger.debug("COMBAT", "CombatSystem initialized: " + player.getName() + " vs " + monster.getName()
                + " | Heal cap: " + combatHealingCap);
    }

    private void initializePlayerAbilities() {
        // The ability loadout system
        // This system was designed with a skill tree concept in mind, but time is
        // limited, so instead we have a level based system.
        // As the player gets more levels, they unlock some more powerful abilities.
        // However, this isn't exactly great.
        // A better system would allow players to choose which abilities to equip, but
        // for now this works.
        // There are some issues with this system, such as lack of customization and
        // player choice.
        // Though with some more time, a better system could be done. Perhaps a month or
        // two.
        playerAbilities = new DynArray<>();

        // Level 1 stuff
        playerAbilities.add(new Ability("Slash", "Basic attack, reliable", 10, 6, 12, false));
        playerAbilities.add(new Ability("Defend", "Reduce damage taken this turn", 12, 0, 0, true));
        playerAbilities.add(new Ability("Rest", "Recover stamina (50% of max)", 5, 0, 0, true));

        // Level 5 stuff
        // Unlock as player levels up
        if (player.getLevel() >= 5) {
            playerAbilities.add(new Ability("Power Strike", "Heavy attack, high risk/reward", 20, 14, 28, false));
        }

        if (player.getLevel() >= 10) {
            playerAbilities.add(new Ability("Parry", "Block next attack and counter", 8, 0, 0, true));
        }

        if (player.getLevel() >= 15) {
            playerAbilities.add(new Ability("Focus", "Next attack deals 25% more damage", 20, 0, 0, true));
        }

        if (player.getLevel() >= 20) {
            playerAbilities.add(new Ability("Execute", "Extra damage if enemy is weak", 18, 10, 20, false));
        }

        if (player.getLevel() >= 30) {
            playerAbilities.add(new Ability("Combo Finisher", "Consume combo for huge damage", 15, 12, 24, false));
        }

        // Only use first 6 abilities
        // This prevents having too many choices and forces strategic selection
        // Does this mean that the player can't use higher level abilities if they have
        // lower level ones?
        // Yes.
        // Could I fix this ?
        // Yes.
        // Will I fix this ?
        // No - the UI / view code is a pain to change / mess around with so it's just
        // not worth it at all.
        DynArray<Ability> capped = new DynArray<>();
        for (int i = 0; i < Math.min(6, playerAbilities.size()); i++) {
            capped.add(playerAbilities.get(i));
        }
        playerAbilities = capped;

        logger.debug("COMBAT", "Initialized player abilities. Count: " + playerAbilities.size() + " (Level "
                + player.getLevel() + ")");
    }

    // Note: A smarter design would give each ability an ID or array index to
    // reference
    // instead of relying on ability names. This works but is fragile - refactoring
    // names breaks things.
    // For now, finding abilities by name is functional enough for this project.
    private int[] estimateDamageRange(Ability ability) {
        // Calculates min/max damage for an ability considering all scaling factors
        // Shows player expected damage range for informed decision-making

        // ability + (strength/2 capped at +20) + weapon, scaled by speed = lineargrowth
        // This keeps damage progression meaningful
        int weaponDamage = 0;
        double speedMult = 1.0;
        Item weapon = player.getEquippedWeapon();
        if (weapon != null) {
            weaponDamage = weapon.getDamage();
            if (weapon.getType() != null) {
                // Different weapon types have different attack speeds
                // Daggers are fast (0.9x damage) but let you attack more often
                // Greatswords are slow (1.1x damage) but hit harder per swing
                // Reduced from 0.8-1.2 to prevent too much variance
                double baseMult = weapon.getType().speedMultiplier;
                speedMult = Math.max(0.8, Math.min(baseMult, 1.15)); // Clamp between 0.8-1.15
            } // What does "weapon speed" do ?
              // It modifies damage output. Is it a good system ? No.
        }

        // Add strength bonus with cap, then multiply by speed
        // Strength contribution capped at +35 to prevent late-game trivializing
        int strengthBonus = Math.min(player.getStrength() / 2, 35);
        int baseMin = (int) ((ability.getMinDamage() + strengthBonus + weaponDamage) * speedMult);
        int baseMax = (int) ((ability.getMaxDamage() + strengthBonus + weaponDamage) * speedMult);

        String abilityName = ability.getName().toLowerCase();

        if (abilityName.contains("power")) {
            baseMin = (int) Math.max(1, baseMin * 0.7); // 30% lower floor
            baseMax = (int) (baseMax * 1.3); // 30% higher ceiling
        }

        if (abilityName.contains("combo")) {
            baseMin += comboCount * 3;
            baseMax += comboCount * 8; // Higher scaling on max damage
        }

        // Execute: extra damage if monster is low health (shown in range)
        if (abilityName.contains("execute")) {
            baseMax = (int) (baseMax * 1.25); // 25% bonus potential if enemy is vulnerable
        }

        return new int[] { Math.max(1, baseMin), Math.max(1, baseMax) };
    }

    private void handleFirstStrike() {
        // Determines who gets a damage bonus on their first attack based on speed
        // difference
        // If one character has significantly higher speed, they get a +15% damage boost
        // on first attack
        // This rewards speed investment without giving cheap free attacks

        int speedDiff = player.getSpeed() - monster.getSpeed();
        if (Math.abs(speedDiff) < 3) {
            return; // Speed difference too small to matter; no decisive advantage
        }

        boolean playerActsFirst = speedDiff > 0;

        if (playerActsFirst) {
            // Player's speed advantage gives them damage bonus on first attack
            playerHasFirstStrikeBonus = true;
            comboCount = 0; // Reset combo
            view.clear();
            String preamble = ConsoleColor.BRIGHT_GREEN + "➤ Quick reflexes! " + player.getName()
                    + " will deal +15% damage on their first attack!" + ConsoleColor.RESET;
            view.showCombatState(player, monster, false, preamble,
                    comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                    monsterBleedTurns, monsterPoisonTurns);
        } else {
            // Monster's speed advantage gives them damage bonus on first attack
            monster.setHasFirstStrikeDamageBonus(true);
            view.clear();
            String preamble = ConsoleColor.BRIGHT_RED + "➤ Ambushed! " + monster.getName()
                    + " will deal +15% damage on their first attack!" + ConsoleColor.RESET;
            view.showCombatState(player, monster, true, preamble,
                    comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                    monsterBleedTurns, monsterPoisonTurns);
        }

        view.pressAnyKey();
    }

    public void start() {
        // Main combat loop: turn-based battle until one combatant is defeated
        // Order: display header -> handle first strike -> alternate turns -> declare
        // winner

        logger.info("COMBAT", "Combat starting");
        try {
            view.clear();
            view.printCombatHeader(player, monster);
            view.pressAnyKey();

            // First strike bonus for high speed characters (established before main turn
            // loop)
            handleFirstStrike();

            showPartyReadinessReport();

            // Determine turn order: whoever has higher speed goes first
            // > If tied, randomly choose
            boolean playerTurn = player.getSpeed() == monster.getSpeed()
                    ? rng.nextInt(2) == 0
                    : player.getSpeed() > monster.getSpeed();
            logger.debug("COMBAT", "First turn: " + (playerTurn ? "Player" : "Monster"));

            int turnCount = 0;
            while ((player.isAlive() || hasAliveCompanion()) && monster.isAlive()) {
                turnCount++;
                logger.debug("COMBAT", "=== Turn " + turnCount + " ===");

                // Tick potion HoT effects at start of turn
                String hotMessage = player.tickPotionHoT();
                if (!hotMessage.isEmpty()) {
                    view.printLine(hotMessage);
                }

                view.clear();

                if (!player.isAlive() && hasAliveCompanion()) {
                    playerTurn = true;
                }

                if (playerTurn && player.isAlive()) {
                    playerTurn();
                } else if (playerTurn) {
                    executeCompanionPhase();
                } else {
                    monsterTurn();
                }

                // Check if combat should end after this turn
                if (!monster.isAlive() || (!player.isAlive() && !hasAliveCompanion())) {
                    break;
                }

                playerTurn = !playerTurn;

                try {
                    // Small delay between turns for readability
                    // Note: This can break the game if interrupted, which is why we try-catch it
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("COMBAT", "Combat interrupted");
                    break;
                }
            }

            // Combat is over; handle consequences
            endCombat();
        } catch (Exception e) {
            logger.error("COMBAT", "Fatal error during combat", e);
            view.showError("Combat error: " + e.getMessage());
            view.pressAnyKey();
        }
    }

    private void playerTurn() {
        logger.debug("COMBAT", "Player's turn");

        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_CYAN + "╔══ YOUR TURN ═══════════════════════════════════════════════════╗"
                + ConsoleColor.RESET);

        // Display all available abilities with their stats
        double weaponRarityMult = player.getEquippedWeapon() != null
                ? player.getEquippedWeapon().getRarity().statMultiplier
                : 1.0;

        for (int i = 0; i < playerAbilities.size(); i++) {
            Ability ability = playerAbilities.get(i);
            int[] range = estimateDamageRange(ability);
            int scaledCost = ability.getScaledStaminaCost(weaponRarityMult);

            String statDisplay;
            if (ability.isDefensive()) {
                statDisplay = "(Utility)";
            } else {
                statDisplay = String.format("(%d-%d dmg)", range[0], range[1]);
            }

            String abilityLine = String.format("║ [%d] %-18s %-12s Cost: %2d SP              ║",
                    i + 1, ability.getName(), statDisplay, scaledCost);
            view.printLine(ConsoleColor.BRIGHT_CYAN + abilityLine + ConsoleColor.RESET);
        }

        // Show potion options
        int optionIndex = playerAbilities.size() + 1;
        int hpCount = countPotions(ItemType.POTION_HEALTH);
        int stCount = countPotions(ItemType.POTION_STAMINA);

        String hpLine = String.format("║ [%d] %-18s %-12s Available: %2d              ║",
                optionIndex, "Use Health Potion", "(+HP)", hpCount);
        view.printLine(ConsoleColor.BRIGHT_CYAN + hpLine + ConsoleColor.RESET);
        optionIndex++;
        String stLine = String.format("║ [%d] %-18s %-12s Available: %2d              ║",
                optionIndex, "Use Stamina Potion", "(+ST)", stCount);
        view.printLine(ConsoleColor.BRIGHT_CYAN + stLine + ConsoleColor.RESET);
        // Mana disabled: remove mana potion option
        optionIndex++;

        // Weapon switching option
        String weaponLine = String.format("║ [%d] %-18s %-12s                            ║",
                optionIndex, "Switch Weapon", "(Equipment)");
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + weaponLine + ConsoleColor.RESET);
        optionIndex++;

        // Flee option - DISABLED
        /*
         * String fleeLine =
         * String.format("║ [%d] %-18s %-12s                            ║",
         * optionIndex, "Flee Combat", "(Escape)");
         * view.printLine(ConsoleColor.BRIGHT_YELLOW + fleeLine + ConsoleColor.RESET);
         */

        view.printLine(ConsoleColor.BRIGHT_CYAN + "╚═══════════════════════════════════════════════════════════════╝"
                + ConsoleColor.RESET);
        view.print(ConsoleColor.BRIGHT_YELLOW + "Choose ability (or 'H' for quick heal) > " + ConsoleColor.RESET);

        // The choice loop
        // This system takes the player choice and tries to execute it if it is valid
        // then it loops again as long as either player or enemy is alive
        // There's a better way to do this.
        boolean validChoice = false;
        while (!validChoice) {
            try {
                String input = view.readLine().trim();
                logger.debug("COMBAT", "Player input: " + input);

                if (input.isEmpty()) {
                    view.showError("Please enter a number!");
                    continue;
                }

                // Quick health potion hotkey
                if (input.equalsIgnoreCase("H")) {
                    String action = usePotion(ItemType.POTION_HEALTH);
                    view.clear();
                    view.showCombatState(player, monster, false, action,
                            comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                            monsterBleedTurns, monsterPoisonTurns);
                    view.pressAnyKey();
                    validChoice = true;
                    continue;
                }

                int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= playerAbilities.size()) {
                    Ability chosen = playerAbilities.get(choice - 1);
                    logger.debug("COMBAT", "Player chose ability: " + chosen.getName());
                    executePlayerAbility(chosen);
                    validChoice = true;
                } else if (choice == playerAbilities.size() + 1) {
                    // Health potion
                    String action = usePotion(ItemType.POTION_HEALTH);
                    view.clear();
                    view.showCombatState(player, monster, false, action,
                            comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                            monsterBleedTurns, monsterPoisonTurns);
                    view.pressAnyKey();
                    validChoice = true;
                } else if (choice == playerAbilities.size() + 2) {
                    // Stamina potion
                    String action = usePotion(ItemType.POTION_STAMINA);
                    view.clear();
                    view.showCombatState(player, monster, false, action,
                            comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                            monsterBleedTurns, monsterPoisonTurns);
                    view.pressAnyKey();
                    validChoice = true;
                } else if (choice == playerAbilities.size() + 3) {
                    // Switch weapon
                    String action = switchWeaponInCombat();
                    view.clear();
                    view.showCombatState(player, monster, false, action,
                            comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                            monsterBleedTurns, monsterPoisonTurns);
                    view.pressAnyKey();
                    validChoice = true;
                } /*
                   * FLEE OPTION DISABLED
                   * else if (choice == playerAbilities.size() + 4) {
                   * // Flee attempt
                   * attemptPlayerFlee();
                   * validChoice = true;
                   * }
                   */ else {
                    view.showError("Invalid choice: " + input + " (1-" + (playerAbilities.size() + 4) + ")");
                }
            } catch (NumberFormatException e) {
                logger.warn("COMBAT", "Invalid input from player: not a number");
                view.showError("Please enter a number! (input was not numeric)");
            } catch (Exception e) {
                logger.error("COMBAT", "Error reading player input", e);
                view.showError("Input error: " + e.getMessage());
                // Default to first ability as fallback
                executePlayerAbility(playerAbilities.get(0));
                validChoice = true;
            }
        }

        executeCompanionPhase();
    }

    private boolean hasAliveCompanion() {
        for (int i = 0; i < partyMembers.size(); i++) {
            if (partyMembers.get(i).isAlive() && partyMembers.get(i).isEngaged()) {
                return true;
            }
        }
        return false;
    }

    private Companion getRandomAliveCompanion() {
        DynArray<Companion> alive = new DynArray<>();
        for (int i = 0; i < partyMembers.size(); i++) {
            Companion companion = partyMembers.get(i);
            if (companion.isAlive() && companion.isEngaged()) {
                alive.add(companion);
            }
        }
        if (alive.size() == 0) {
            return null;
        }
        return alive.get(rng.nextInt(alive.size()));
    }

    private void executeCompanionPhase() {
        if (!monster.isAlive() || partyMembers.size() == 0) {
            return;
        }

        int partyAverageCohesion = getPartyAverageCohesion();
        int monsterThreat = calculateMonsterThreat();

        if (monster.isBoss() && partyAverageCohesion < 35) {
            forcePartyDisengage("Party cohesion collapsed under boss pressure");
        }

        StringBuilder phaseLog = new StringBuilder();
        for (int i = 0; i < partyMembers.size(); i++) {
            Companion companion = partyMembers.get(i);
            if (!companion.isAlive() || !monster.isAlive() || !companion.isEngaged()) {
                continue;
            }

            if (companion.shouldDisengage(monsterThreat, monster.isBoss(), partyAverageCohesion)) {
                companion.disengage(companion.getDisengageReason());
                phaseLog.append("\n").append(ConsoleColor.BRIGHT_RED)
                        .append("➤ ").append(companion.getName()).append(" disengaged: ")
                        .append(companion.getDisengageReason())
                        .append(ConsoleColor.RESET);
                continue;
            }

            double playerHealthRatio = player.getHealth() / (double) Math.max(1, player.getMaxHealth());
            Companion.CompanionAction action = companion.chooseAction(playerHealthRatio, monsterThreat);

            switch (action) {
                case RETREAT -> {
                    companion.disengage("Loyalty/Cohesion threshold failed vs threat");
                    phaseLog.append("\n").append(ConsoleColor.BRIGHT_RED)
                            .append("➤ ").append(companion.getName())
                            .append(" withdrew from combat (morale check)")
                            .append(ConsoleColor.RESET);
                }
                case HEAL, SUPPORT -> {
                    int supportHeal = companion.trySupportHeal(playerHealthRatio);
                    if (supportHeal > 0 && player.isAlive()) {
                        healWithCap(supportHeal);
                        phaseLog.append("\n").append(ConsoleColor.BRIGHT_CYAN)
                                .append("➤ ").append(companion.getName()).append(" heals/supports (+")
                                .append(supportHeal).append(" HP)")
                                .append(ConsoleColor.RESET);
                    }
                }
                case ATTACK -> {
                    int companionDamage = companion.performAttack(rng);
                    if (monster.isDefending()) {
                        companionDamage = (int) (companionDamage * 0.7);
                        monster.setDefending(false);
                    }
                    if (monster.isCounterStanceActive()) {
                        int reflected = Math.max(1, companionDamage / 4);
                        companion.takeDamage(reflected);
                        monster.setCounterStanceActive(false);
                    }
                    monster.takeDamage(companionDamage);
                    phaseLog.append("\n").append(ConsoleColor.BRIGHT_GREEN)
                            .append("➤ ").append(companion.getName()).append(" strikes for ")
                            .append(companionDamage).append(" damage!")
                            .append(ConsoleColor.RESET);
                }
                case HOLD -> {
                    companion.restoreStamina(8);
                    phaseLog.append("\n").append(ConsoleColor.BRIGHT_YELLOW)
                            .append("➤ ").append(companion.getName()).append(" holds position and regains stamina")
                            .append(ConsoleColor.RESET);
                }
            }
        }

        if (!phaseLog.isEmpty() && monster.isAlive()) {
            view.clear();
            view.showCombatState(player, monster, false,
                    ConsoleColor.BRIGHT_WHITE + "Allies act!" + ConsoleColor.RESET + phaseLog);
            view.pressAnyKey();
        }
    }

    private int calculateMonsterThreat() {
        return (monster.getLevel() * 10) + monster.getDamage() + (monster.getDefense() * 2);
    }

    private int getPartyAverageCohesion() {
        if (partyMembers.size() == 0) {
            return 100;
        }
        int total = 0;
        int count = 0;
        for (int i = 0; i < partyMembers.size(); i++) {
            Companion companion = partyMembers.get(i);
            if (companion.isAlive()) {
                total += companion.getCohesion();
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return total / count;
    }

    private void forcePartyDisengage(String reason) {
        for (int i = 0; i < partyMembers.size(); i++) {
            Companion companion = partyMembers.get(i);
            if (companion.isAlive() && companion.isEngaged()) {
                companion.disengage(reason);
            }
        }
    }

    private void showPartyReadinessReport() {
        if (partyMembers.size() == 0) {
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append(ConsoleColor.BRIGHT_WHITE).append("Party Readiness:").append(ConsoleColor.RESET);
        int threat = calculateMonsterThreat();
        int avgCohesion = getPartyAverageCohesion();
        report.append("\n")
                .append(ConsoleColor.BRIGHT_YELLOW)
                .append("Threat Score: ").append(threat)
                .append(" | Avg Cohesion: ").append(avgCohesion)
                .append(ConsoleColor.RESET);

        for (int i = 0; i < partyMembers.size(); i++) {
            Companion companion = partyMembers.get(i);
            report.append("\n").append(" - ").append(companion.getMoraleSummary());
        }

        if (monster.isBoss() && avgCohesion < 35) {
            report.append("\n")
                    .append(ConsoleColor.BRIGHT_RED)
                    .append("WARNING: Cohesion too low for this boss. Companions may disengage.")
                    .append(ConsoleColor.RESET);
        }

        view.clear();
        view.showCombatState(player, monster, false, report.toString());
        view.pressAnyKey();
    }

    private int countPotions(ItemType type) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getType() == type)
                count++;
        }
        return count;
    }

    private String usePotion(ItemType type) {
        // Find first potion of the specified type
        int index = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getType() == type) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return ConsoleColor.BRIGHT_YELLOW + "✖ No " + type.displayName + "!" + ConsoleColor.RESET;
        }

        Item potion = player.getInventory().get(index);
        // NEW POTION SYSTEM: Calculate HoT values based on player stats
        int[] hotValues = potion.getPotionHoTValues(player.getIntelligence(), player.getLevel(), player.getLuck());
        int initialHeal = hotValues[0];
        int hotTotal = hotValues[1];
        int hotTurns = hotValues[2];

        String actionText;

        switch (type) {
            case POTION_HEALTH -> {
                // Apply immediate heal + HoT effect
                healWithCap(initialHeal); // Apply combat healing cap to initial heal
                player.applyPotionHoT(0, hotTotal, hotTurns, true); // HoT not affected by combat cap
                actionText = ConsoleColor.BRIGHT_GREEN + "✓ Used Health Potion! (+" + initialHeal + " HP now, +"
                        + hotTotal + " HP over " + hotTurns + " turns)" + ConsoleColor.RESET;
            }
            case POTION_STAMINA -> {
                player.applyPotionHoT(initialHeal, hotTotal, hotTurns, false);
                actionText = ConsoleColor.BRIGHT_CYAN + "✓ Used Stamina Potion! (+" + initialHeal + " ST now, +"
                        + hotTotal + " ST over " + hotTurns + " turns)" + ConsoleColor.RESET;
            }
            default -> {
                actionText = ConsoleColor.BRIGHT_YELLOW + "✖ Cannot use that potion" + ConsoleColor.RESET;
            }
        }

        // Remove potion from inventory
        player.getInventory().remove(index);

        return actionText;
    }

    private String switchWeaponInCombat() {
        // Display available weapons in inventory
        DynArray<Item> weapons = new DynArray<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().get(i);
            if (item.getType().isWeapon()) {
                weapons.add(item);
            }
        }

        if (weapons.size() == 0) {
            return ConsoleColor.BRIGHT_YELLOW + "✖ No weapons in inventory!" + ConsoleColor.RESET;
        }

        view.clear();
        view.printLine("");
        view.printLine(ConsoleColor.BRIGHT_MAGENTA
                + "╔══ SWITCH WEAPON ═══════════════════════════════════════════════╗" + ConsoleColor.RESET);

        // Show current weapon
        String currentWeaponName = player.getEquippedWeapon() != null
                ? player.getEquippedWeapon().getName()
                : "None";
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "║ Current: " + currentWeaponName + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "║" + ConsoleColor.RESET);

        // Show available weapons
        for (int i = 0; i < weapons.size(); i++) {
            Item weapon = weapons.get(i);
            String weaponLine = String.format("║ [%d] %-30s %s",
                    i + 1,
                    weapon.getName(),
                    weapon.getRarity().getColoredName());
            view.printLine(ConsoleColor.BRIGHT_MAGENTA + weaponLine + ConsoleColor.RESET);
        }

        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "║ [0] Cancel" + ConsoleColor.RESET);
        view.printLine(ConsoleColor.BRIGHT_MAGENTA + "╚═══════════════════════════════════════════════════════════════╝"
                + ConsoleColor.RESET);
        view.print(ConsoleColor.BRIGHT_YELLOW + "Choose weapon > " + ConsoleColor.RESET);

        try {
            String input = view.readLine();
            int choice = Integer.parseInt(input);

            if (choice == 0) {
                return ConsoleColor.BRIGHT_YELLOW + "✖ Cancelled weapon switch" + ConsoleColor.RESET;
            }

            if (choice > 0 && choice <= weapons.size()) {
                Item newWeapon = weapons.get(choice - 1);

                // Put current weapon back in inventory if exists
                if (player.getEquippedWeapon() != null) {
                    player.addItem(player.getEquippedWeapon());
                }

                // Remove new weapon from inventory and equip it
                player.removeItem(newWeapon);
                player.equipWeapon(newWeapon);

                return ConsoleColor.BRIGHT_GREEN + "✓ Switched to " + newWeapon.getName() + "!" + ConsoleColor.RESET;
            } else {
                return ConsoleColor.BRIGHT_YELLOW + "✖ Invalid choice!" + ConsoleColor.RESET;
            }
        } catch (Exception e) {
            return ConsoleColor.BRIGHT_RED + "✖ Error switching weapon!" + ConsoleColor.RESET;
        }
    }

    private void executePlayerAbility(Ability ability) {
        // Calculate scaled stamina cost based on weapon rarity
        double weaponRarityMult = player.getEquippedWeapon() != null
                ? player.getEquippedWeapon().getRarity().statMultiplier
                : 1.0;
        int scaledCost = ability.getScaledStaminaCost(weaponRarityMult);

        // Central method that executes player abilities in combat
        // Handles different ability types (defensive, special, offensive) with their
        // unique mechanics
        // Damage calculation chain: base_ability_damage -> weapon_speed -> focus_buff
        // -> combo_scaling ->
        // weakness_multiplier -> elemental_affinity -> weapon_traits ->
        // execute_ability_bonus
        //
        // This multi-layer damage system allows for many different playstyles:
        // - Weapon choice (daggers vs greatswords) affects both speed and combo
        // building
        // - Focus + combo can stack for explosive damage
        // - Elemental matching rewards planning ahead
        // - Weapon traits add tactical elements (bleed for DoT, vampiric for sustain,
        // etc.)

        if (player.getStamina() < scaledCost) {
            view.printLine("");
            view.printColoredLine("✖ Not enough stamina!", ConsoleColor.BRIGHT_RED); // I do quite like coloured text
            view.pressAnyKey();
            return;
        }

        player.useStamina(scaledCost);

        String actionResult = "";

        if (ability.getName().contains("Parry")) {
            // Parry ability - block next turn and counter if low HP
            playerParryBlock = 1;
            actionResult = ConsoleColor.BRIGHT_BLUE + "➤ " + player.getName() + " took a defensive parry stance!"
                    + ConsoleColor.RESET;
            comboCount = 0;
        } else if (ability.getName().contains("Rest")) {
            // Rest ability - recover stamina based on difficulty
            int baseRestore = (int) (player.getMaxStamina() * 0.50);

            // Difficulty scaling - harder difficulties restore less stamina
            // Logic: On easy/common monsters you recover 50% stamina, but on legendary
            // monsters only 20%
            // This prevents Rest from trivializing hard fights (you can't just spam Rest
            // indefinitely)
            double difficultyMod = switch (monster.getRarity()) {
                case COMMON, UNCOMMON -> 1.0;
                case RARE -> 0.6;
                case MYTHICAL, LEGENDARY, DEBUG -> 0.4;
            };

            int restoreAmount = (int) (baseRestore * difficultyMod);
            player.restoreStamina(restoreAmount);
            actionResult = ConsoleColor.BRIGHT_CYAN + "➤ " + player.getName() + " rested and recovered "
                    + restoreAmount + " stamina!" + ConsoleColor.RESET;
            comboCount = 0;
        } else if (ability.getName().contains("Focus")) {
            // Focus ability - next attack deals 50% more damage
            // This is a set-up ability: use Focus this turn, then attack next turn for
            // bonus damage
            // Synergizes with slow weapons (greatswords) to maximize the bonus on a
            // powerful hit
            playerFocused = true;
            actionResult = ConsoleColor.BRIGHT_YELLOW + "⚡ " + player.getName()
                    + " focused their energy! Next attack boosted!"
                    + ConsoleColor.RESET;
            comboCount = 0;
        } else if (ability.isDefensive()) {
            // Generic defensive ability handling (Defend)
            comboCount = 0;
            actionResult = ConsoleColor.BRIGHT_BLUE + "➤ " + player.getName() + " used " + ability.getName() + "!"
                    + ConsoleColor.RESET;
        } else {
            // ===== OFFENSIVE ABILITY EXECUTION =====
            // Multi-stage damage calculation: base -> scaling -> buffs -> weakness ->
            // traits -> finishers

            Item weapon = player.getEquippedWeapon();
            double weaponSpeed = weapon != null && weapon.getType() != null ? weapon.getType().speedMultiplier : 1.0;

            // Combo building: faster weapons accumulate combo faster
            // Formula: add 1 + (weapon_speed_bonus * 2) per attack
            // Example: Dagger (0.8x speed) adds 1 combo, Greatsword (1.2x speed) adds
            // 1+0.4*2=1.4→1 combo
            // Wait that math doesn't seem right... TODO: verify combo progression feels
            // balanced with weapon speeds
            int comboStep = 1 + (int) Math.floor(Math.max(0, (weaponSpeed - 1.0) * 2));
            comboCount += comboStep;

            // Calculate base damage from ability and player strength
            // Weapon speed is a multiplier, so faster weapons deal more damage with the
            // same ability
            // This makes weapon choice matter: faster weapons for consistent damage, heavy
            // for big hits
            int damage = (int) ((ability.calculateDamage(rng) + player.getStrength()) * weaponSpeed);

            // Apply Focus buff if active (25% damage increase with higher stamina cost)
            // BALANCED: Reduced from 50% to 25% and increased stamina cost from 12 to 20
            // This prevents focus-spam one-shots while maintaining tactical importance
            // Focus is consumed after being applied, so you only get one high-damage hit
            // per Focus cast
            if (playerFocused) {
                int focusBonus = (int) (damage * 0.25); // Reduced from 0.50 → 0.35 → 0.25
                damage += focusBonus;
                actionResult = ConsoleColor.BRIGHT_YELLOW + "✦ FOCUSED STRIKE! +" + focusBonus + " damage! ";
                playerFocused = false; // Consume the focus buff
            }

            // Combo scaling (builds up with consecutive attacks)
            // BALANCED: Reduced from 2x to 1x per count to prevent exponential combos
            // Each attack adds to combo count, scaling damage by 1 point per count
            // Example: 5 consecutive attacks = 15 combo count = 15 bonus damage (was 30)
            // This rewards attacking repeatedly but prevents one-shot potential
            int comboBonus = comboCount * 1; // Reduced from 2
            if (ability.getName().contains("Combo")) {
                // Combo Finisher consumes the entire combo streak for massive damage
                // BALANCED: Reduced finisher bonus from 5x to 2x
                // At 5 combo: base 10 damage + finisher 10 = 20 total (instead of 55)
                comboBonus += comboCount * 2; // Reduced from 3
                comboCount = 0;
            }
            damage += comboBonus;

            // Apply monster's elemental weakness
            // Each monster has a weakness element that boosts damage against them
            // Matches rock-paper-scissors style: Fire > Cold > Lightning > Fire
            damage = (int) (damage * monster.getWeakness().getDamageMultiplier(monster.getType()));

            // Elemental affinity bonus (if player's element matches monster's weakness)
            // BALANCED: Reduced from 30% to 20% to prevent stacking too much damage
            // Additional 20% bonus if player spec'd into the right element for this fight
            // Rewards planning: "This floor has lots of fire enemies, so I should equip
            // fire gear"
            // But doesn't make the fight trivial
            if (player.getElementalAffinity() != null && monster.getWeakness() == player.getElementalAffinity()) {
                int elementalBonus = (int) (damage * 0.20); // Reduced from 0.30
                damage += elementalBonus;
                actionResult += ConsoleColor.BRIGHT_YELLOW + "[ELEMENTAL +" + elementalBonus + "]" + ConsoleColor.RESET;
            }

            // Apply weapon traits (Bleed, Vampiric, Execute, Swift, Cursed, Blessed,
            // Corrupting)
            // REVAMPED: Expanded trait system with more interesting mechanics
            // Each weapon trait adds a unique strategic element to combat
            String traitBonus = "";
            if (weapon != null && weapon.getTrait() != null) {
                switch (weapon.getTrait()) {
                    case BLEED -> {
                        int bleed = Math.max(1, (int) (damage * 0.12));
                        damage += bleed;
                        monsterBleedTurns = 2;
                        traitBonus = ConsoleColor.BRIGHT_MAGENTA + " [BLEED +" + bleed + "]" + ConsoleColor.RESET;
                    }
                    case VAMPIRIC -> {
                        int heal = Math.max(1, (int) (damage * 0.10));
                        healWithCap(heal);
                        traitBonus = ConsoleColor.BRIGHT_GREEN + " [VAMPIRIC +" + heal + " HP]" + ConsoleColor.RESET;
                    }
                    case EXECUTE -> {
                        if (monster.getHealth() < monster.getMaxHealth() * 0.35) {
                            int exec = Math.max(1, (int) (damage * 0.15));
                            damage += exec;
                            traitBonus = ConsoleColor.BRIGHT_YELLOW + " [EXECUTE +" + exec + "]" + ConsoleColor.RESET;
                        }
                    }
                    case SWIFT -> {
                        player.restoreStamina(5);
                        traitBonus = ConsoleColor.BRIGHT_CYAN + " [SWIFT +5 ST]" + ConsoleColor.RESET;
                    }
                    case CURSED -> {
                        // REVAMPED: Cursed weapons scale inversely with player health
                        // Below 75%: Extremely powerful, deals 1-4x damage based on desperation
                        // Above 75%: Backfires! Heals the enemy instead of dealing damage
                        double healthRatio = player.getHealth() / (double) player.getMaxHealth();

                        if (healthRatio < 0.75) {
                            // Desperation mode: scales multiplicatively with missing health
                            // At 75% health = no bonus (1.0x)
                            // At 50% health = +25% damage (1.25x)
                            // At 25% health = +75% damage (1.75x)
                            // At 1% health = +99% damage (1.99x)
                            double desperationMult = 1.0 + ((0.75 - healthRatio) / 0.75);
                            int cursedBonus = (int) (damage * (desperationMult - 1.0));
                            damage = (int) (damage * desperationMult);
                            traitBonus = ConsoleColor.BRIGHT_RED + " [CURSED DESPERATION +" + cursedBonus + "]"
                                    + ConsoleColor.RESET;
                        } else {
                            // Overconfidence penalty: weapon heals enemy when player is too healthy
                            // Deals only 50% damage and heals enemy for 10% of intended damage
                            int enemyHeal = Math.max(1, (int) (damage * 0.10));
                            damage = (int) (damage * 0.50);
                            monster.heal(enemyHeal);
                            traitBonus = ConsoleColor.BRIGHT_RED + " [CURSED BACKFIRE! -" + damage + " DMG, HEALED "
                                    + enemyHeal + "]" + ConsoleColor.RESET;
                        }
                    }
                    case BLESSED -> {
                        // REVAMPED: Blessed weapons provide protective effects
                        // Grants damage reflection (20% chance to reflect damage)
                        // Always heals for 5% of damage dealt
                        int blessedHeal = Math.max(1, (int) (damage * 0.05));
                        healWithCap(blessedHeal);

                        // 20% chance for damage reflection
                        if (rng.nextInt(100) < 20) {
                            int reflection = Math.max(1, damage / 4);
                            monster.takeDamage(reflection);
                            traitBonus = ConsoleColor.BRIGHT_YELLOW + " [BLESSED: HEALED +" + blessedHeal
                                    + " HP, REFLECTED " + reflection + "]" + ConsoleColor.RESET;
                        } else {
                            traitBonus = ConsoleColor.BRIGHT_YELLOW + " [BLESSED: HEALED +" + blessedHeal + " HP]"
                                    + ConsoleColor.RESET;
                        }
                    }
                    case CORRUPTING -> {
                        // REVAMPED: Corrupting weapons grow stronger with each hit
                        // Uses combo count: each combo adds 3% damage multiplier
                        // Encourages aggressive play and consecutive hits
                        double corruptBonus = 1.0 + (comboCount * 0.03);
                        int bonusDamage = (int) (damage * (corruptBonus - 1.0));
                        damage = (int) (damage * corruptBonus);
                        traitBonus = ConsoleColor.BRIGHT_MAGENTA + " [CORRUPTING +3% x" + comboCount + " = +"
                                + bonusDamage + "]" + ConsoleColor.RESET;
                    }
                    default -> {
                    }
                }
            }

            // Handle Execute ability (extra damage against low HP enemies)
            // Similar to Execute trait but for the ability instead of weapon
            // Stacks with Execute trait: you can get 25% bonus from both trait AND ability
            // on the same turn
            // This creates finisher opportunities: hunt low HP enemies for maximum Execute
            // synergy
            if (ability.getName().contains("Execute")) {
                if (monster.getHealth() < monster.getMaxHealth() * 0.35) {
                    int executeBonus = (int) (damage * 0.25);
                    damage += executeBonus;
                    traitBonus += ConsoleColor.BRIGHT_RED + " [CRITICAL +" + executeBonus + "]" + ConsoleColor.RESET;
                }
            }

            // Get damage breakdown BEFORE applying damage
            int[] breakdown = monster.getDamageBreakdown(damage);
            int baseDamage = breakdown[0];
            int actualDamage = breakdown[1];
            int defenseReduction = breakdown[2];
            int defensePercent = breakdown[3];

            if (monster.isDefending()) {
                damage = (int) (damage * 0.7);
                monster.setDefending(false);
            }

            if (monster.isCounterStanceActive()) {
                int reflectedDamage = Math.max(1, damage / 5);
                player.takeDamage(reflectedDamage);
                traitBonus += ConsoleColor.BRIGHT_RED + " [COUNTER REFLECT " + reflectedDamage + "]"
                        + ConsoleColor.RESET;
                monster.setCounterStanceActive(false);
            }

            monster.takeDamage(damage);

            // Build damage breakdown text
            String defenseBreakdown = "";
            if (defenseReduction > 0) {
                defenseBreakdown = ConsoleColor.BRIGHT_YELLOW + " (-" + defensePercent + "% defense = "
                        + actualDamage + " taken)" + ConsoleColor.RESET;
            }

            // Apply first strike damage bonus if this is player's first attack and they
            // have the bonus
            String firstStrikeText = "";
            if (playerHasFirstStrikeBonus) {
                int bonusDamage = (int) (damage * FIRST_STRIKE_DAMAGE_BONUS);
                int[] bonusBreakdown = monster.getDamageBreakdown(bonusDamage);
                int bonusActualDamage = bonusBreakdown[1];
                monster.takeDamage(bonusDamage);
                firstStrikeText = ConsoleColor.BRIGHT_YELLOW + " [FIRST STRIKE +" + bonusActualDamage + "]"
                        + ConsoleColor.RESET;
                playerHasFirstStrikeBonus = false; // Consume the bonus
            }

            // Display combo streak indicator
            String comboText = "";
            if (comboBonus > 0) {
                comboText = ConsoleColor.BRIGHT_YELLOW + " (COMBO x" + (1 + comboCount / 3) + " +" + comboBonus + ")"
                        + ConsoleColor.RESET;
            }

            actionResult += ConsoleColor.BRIGHT_GREEN + "➤ " + player.getName() + " dealt " + ConsoleColor.BRIGHT_WHITE
                    + baseDamage + ConsoleColor.BRIGHT_GREEN + " damage!" + ConsoleColor.RESET + comboText
                    + defenseBreakdown + firstStrikeText + traitBonus;
        }

        view.clear();
        view.showCombatState(player, monster, false, actionResult);
        view.pressAnyKey();
    }

    private void monsterTurn() {
        logger.debug("COMBAT", "Monster's turn");

        // FLEE LOGIC DISABLED - Monsters no longer flee from combat
        /*
         * // Cowardice/flee logic - monsters may give up when nearly defeated
         * // Only non-boss enemies can flee (bosses fight to the death)
         * // Condition: health below 15% AND 35% random chance
         * // Example: Monster at 14/100 HP has a 35% chance to flee each turn (65%
         * stays
         * // to fight)
         * // This is more fun than grinding out the last hits and adds element of
         * // chance/mercy
         * // Fleeing enemies still give full combat victory (you win the encounter)
         * // TODO: Could track monster fleeing as a stat (morale) that persists between
         * // encounters
         * if (!monster.isBoss() && monster.getHealth() < monster.getMaxHealth() * 0.15
         * && rng.nextInt(100) < 35) {
         * String fleeText = ConsoleColor.BRIGHT_YELLOW + "➤ " + monster.getName() +
         * " fled the battle!"
         * + ConsoleColor.RESET;
         * monster.takeDamage(monster.getHealth());
         * view.clear();
         * view.showCombatState(player, monster, true, fleeText);
         * view.pressAnyKey();
         * return;
         * }
         */

        // Tick ability cooldowns
        monster.tickAbilityCooldowns();

        // Choose ability to use
        MonsterAbility chosenAbility = monster.chooseAbility();

        String actionResult = "";

        // Monster is resting (no ability chosen)
        if (chosenAbility == null) {
            monster.restoreStamina();
            actionResult = ConsoleColor.BRIGHT_CYAN + "➤ " + monster.getName() + " recovered stamina!"
                    + ConsoleColor.RESET;
            view.clear();
            view.showCombatState(player, monster, true, actionResult);
            view.pressAnyKey();
            return;
        }

        // Check if telegraphing next move
        if (monster.getNextTelegraphedAbility() != null) {
            actionResult = ConsoleColor.BRIGHT_YELLOW + "⚠ " + monster.getName() + " is preparing "
                    + monster.getNextTelegraphedAbility().getName() + "!" + ConsoleColor.RESET;
            view.clear();
            view.showCombatState(player, monster, true, actionResult);
            view.pressAnyKey();
            return;
        }

        // Use the ability
        chosenAbility.use();
        monster.useStamina(chosenAbility.getStaminaCost());

        // Calculate base damage
        int damage = chosenAbility.calculateDamage(rng) + monster.getDamage();

        // Apply enrage modifier
        if (monster.isEnraged()) {
            damage = (int) (damage * 1.5);
        }

        // Apply first strike damage bonus if monster has it
        if (monster.getHasFirstStrikeDamageBonus()) {
            int bonusDamage = (int) (damage * FIRST_STRIKE_DAMAGE_BONUS);
            damage += bonusDamage;
            // Note: the bonus display is handled in the actionResult below
            monster.setHasFirstStrikeDamageBonus(false); // Consume the bonus
        }

        // Some monster attacks can target party members to make combat more dynamic
        if (hasAliveCompanion()
                && chosenAbility.getType() != MonsterAbility.AbilityType.DEFENSIVE_STANCE
                && chosenAbility.getType() != MonsterAbility.AbilityType.COUNTER_STANCE
                && chosenAbility.getType() != MonsterAbility.AbilityType.ENRAGE
                && rng.nextInt(100) < 30) {
            Companion target = getRandomAliveCompanion();
            if (target != null) {
                int companionDamage = Math.max(1, damage - (target.getRole() == Companion.Role.VANGUARD ? 4 : 0));
                if (chosenAbility.getType() == MonsterAbility.AbilityType.MULTI_STRIKE) {
                    companionDamage += Math.max(1, damage / 2);
                }
                target.takeDamage(companionDamage);
                actionResult = ConsoleColor.BRIGHT_RED + "➤ " + monster.getName() + " targeted "
                        + target.getName() + " for " + companionDamage + " damage!" + ConsoleColor.RESET;
                view.clear();
                view.showCombatState(player, monster, true, actionResult,
                        comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                        monsterBleedTurns, monsterPoisonTurns);
                view.pressAnyKey();
                return;
            }
        }

        // Check if player is parrying
        if (playerParryBlock > 0) {
            playerParryBlock--;
            int reducedDamage = (int) (damage * 0.5);
            player.takeDamage(reducedDamage);

            actionResult = ConsoleColor.BRIGHT_BLUE + "➤ " + player.getName() + " parried "
                    + chosenAbility.getName() + "! (Reduced to " + reducedDamage + " damage)" + ConsoleColor.RESET;

            // Counter-attack if low HP
            if (player.getHealth() < player.getMaxHealth() * 0.25) {
                int counterDamage = (int) (monster.getDamage() * 0.75);
                monster.takeDamage(counterDamage);
                actionResult += ConsoleColor.BRIGHT_YELLOW + "\n➤ Counter-attack for " + counterDamage + " damage!"
                        + ConsoleColor.RESET;
            }
        } else {
            // LUCK-BASED DODGE SYSTEM
            // Luck provides dodge chance: 1% per 20 luck (max 5% at 100 luck)
            int dodgeChance = Math.min(5, player.getLuck() / 20);
            boolean dodged = rng.nextInt(100) < dodgeChance;

            if (dodged) {
                actionResult = ConsoleColor.BRIGHT_CYAN + "➜ " + player.getName()
                        + " dodged " + chosenAbility.getName() + "! [LUCK]" + ConsoleColor.RESET;
            } else {
                switch (chosenAbility.getType()) {
                    case BASIC_ATTACK:
                    case HEAVY_STRIKE:
                    case RAPID_STRIKE:
                        int damageReduced = player.getArmorValue();
                        int mitigatedDamage = Math.max(1, damage - damageReduced);
                        int percentReduced = damageReduced > 0 ? (int) ((damageReduced * 100.0) / damage) : 0;
                        player.takeDamage(mitigatedDamage);
                        actionResult = ConsoleColor.BRIGHT_RED + "➤ " + monster.getName() + " used "
                                + chosenAbility.getName() + " for " + ConsoleColor.BRIGHT_WHITE + damage
                                + ConsoleColor.BRIGHT_RED + " damage! " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + percentReduced + "% armor = " + mitigatedDamage + " taken)"
                                + ConsoleColor.RESET;
                        break;

                    case LIFE_DRAIN:
                        int drainDamageReduced = player.getArmorValue();
                        int mitigatedDrainDamage = Math.max(1, damage - drainDamageReduced);
                        int drainPercentReduced = drainDamageReduced > 0 ? (int) ((drainDamageReduced * 100.0) / damage)
                                : 0;
                        player.takeDamage(mitigatedDrainDamage);
                        int heal = mitigatedDrainDamage / 2;
                        monster.heal(heal);
                        actionResult = ConsoleColor.BRIGHT_MAGENTA + "➤ " + monster.getName()
                                + " drained " + damage + " HP " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + drainPercentReduced + "% armor = " + mitigatedDrainDamage + " taken)"
                                + ConsoleColor.BRIGHT_MAGENTA + " and healed " + heal + " HP!" + ConsoleColor.RESET;
                        break;

                    case POISON_ATTACK:
                        int poisonDamageReduced = player.getArmorValue();
                        int mitigatedPoisonDamage = Math.max(1, damage - poisonDamageReduced);
                        int poisonPercentReduced = poisonDamageReduced > 0
                                ? (int) ((poisonDamageReduced * 100.0) / damage)
                                : 0;
                        player.takeDamage(mitigatedPoisonDamage);
                        playerPoisonTurns = 3;
                        actionResult = ConsoleColor.BRIGHT_GREEN + "➤ " + monster.getName()
                                + " poisoned you for " + damage + " damage! " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + poisonPercentReduced + "% armor = " + mitigatedPoisonDamage + " taken)"
                                + ConsoleColor.RESET;
                        break;

                    case BLEED_ATTACK:
                        int bleedDamageReduced = player.getArmorValue();
                        int mitigatedBleedDamage = Math.max(1, damage - bleedDamageReduced);
                        int bleedPercentReduced = bleedDamageReduced > 0 ? (int) ((bleedDamageReduced * 100.0) / damage)
                                : 0;
                        player.takeDamage(mitigatedBleedDamage);
                        playerBleedTurns = 3;
                        actionResult = ConsoleColor.BRIGHT_MAGENTA + "➤ " + monster.getName()
                                + " caused bleeding for " + damage + " damage! " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + bleedPercentReduced + "% armor = " + mitigatedBleedDamage + " taken)"
                                + ConsoleColor.RESET;
                        break;

                    case STUNNING_BLOW:
                        int stunDamageReduced = player.getArmorValue();
                        int mitigatedStunDamage = Math.max(1, damage - stunDamageReduced);
                        int stunPercentReduced = stunDamageReduced > 0 ? (int) ((stunDamageReduced * 100.0) / damage)
                                : 0;
                        player.takeDamage(mitigatedStunDamage);
                        player.useStamina(20); // Stun effect = stamina loss
                        actionResult = ConsoleColor.BRIGHT_YELLOW + "➤ " + monster.getName()
                                + " stunned you for " + damage + " damage! " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + stunPercentReduced + "% armor = " + mitigatedStunDamage + " taken)"
                                + ConsoleColor.RESET + " (-20 stamina)";
                        break;

                    case DEFENSIVE_STANCE:
                        monster.setDefending(true);
                        actionResult = ConsoleColor.BRIGHT_CYAN + "➤ " + monster.getName()
                                + " took a defensive stance!" + ConsoleColor.RESET;
                        break;

                        case COUNTER_STANCE:
                        monster.setDefending(true);
                        monster.setCounterStanceActive(true);
                        actionResult = ConsoleColor.BRIGHT_MAGENTA + "➤ " + monster.getName()
                            + " prepared a counter stance!" + ConsoleColor.RESET;
                        break;

                        case SPELL_CAST:
                        int spellMitigation = Math.max(0, player.getResilience() / 3);
                        int spellDamage = Math.max(1, damage - spellMitigation);
                        player.takeDamage(spellDamage);
                        actionResult = ConsoleColor.BRIGHT_MAGENTA + "➤ " + monster.getName()
                            + " cast " + chosenAbility.getName() + " for " + spellDamage
                            + " arcane damage!" + ConsoleColor.RESET;
                        break;

                        case MULTI_STRIKE:
                        int hitOne = Math.max(1, (damage / 2) - player.getArmorValue());
                        int hitTwo = Math.max(1, (damage / 2) - player.getArmorValue());
                        int total = hitOne + hitTwo;
                        player.takeDamage(total);
                        actionResult = ConsoleColor.BRIGHT_RED + "➤ " + monster.getName()
                            + " used Twin Strike (" + hitOne + " + " + hitTwo + " = " + total + ")!"
                            + ConsoleColor.RESET;
                        break;

                    case ENRAGE:
                        monster.setEnraged(true);
                        actionResult = ConsoleColor.BRIGHT_RED + "➤ " + monster.getName()
                                + " entered a berserker rage!" + ConsoleColor.RESET;
                        break;

                    case BOSS_TELEGRAPH:
                        int bossDamageReduced = player.getArmorValue();
                        int mitigatedBossDamage = Math.max(1, damage - bossDamageReduced);
                        int bossPercentReduced = bossDamageReduced > 0 ? (int) ((bossDamageReduced * 100.0) / damage)
                                : 0;
                        player.takeDamage(mitigatedBossDamage);
                        actionResult = ConsoleColor.BRIGHT_YELLOW + "➤➤ " + monster.getName()
                                + " unleashed " + chosenAbility.getName() + " for " + ConsoleColor.BRIGHT_WHITE
                                + damage + ConsoleColor.BRIGHT_YELLOW + " MASSIVE DAMAGE! " + ConsoleColor.BRIGHT_YELLOW
                                + "(-" + bossPercentReduced + "% armor = " + mitigatedBossDamage + " taken)"
                                + ConsoleColor.RESET;
                        break;

                    default:
                        player.takeDamage(damage);
                        actionResult = ConsoleColor.BRIGHT_RED + "➤ " + monster.getName() + " attacked for "
                                + damage + " damage!" + ConsoleColor.RESET;
                        break;
                }
            } // End dodge check
        }

        // DoT
        if (playerBleedTurns > 0) {
            playerBleedTurns--;
            player.takeDamage(BLEED_DAMAGE);
            actionResult += ConsoleColor.BRIGHT_MAGENTA + "\n➤ Bleeding: " + BLEED_DAMAGE + " damage!"
                    + ConsoleColor.RESET;
        }

        if (playerPoisonTurns > 0) {
            playerPoisonTurns--;
            player.takeDamage(POISON_DAMAGE);
            actionResult += ConsoleColor.BRIGHT_GREEN + "\n➤ Poison: " + POISON_DAMAGE + " damage!"
                    + ConsoleColor.RESET;
        }

        view.clear();
        view.showCombatState(player, monster, true, actionResult,
                comboCount, playerFocused, playerBleedTurns, playerPoisonTurns,
                monsterBleedTurns, monsterPoisonTurns);
        view.pressAnyKey();
    }

    private void endCombat() {
        logger.info("COMBAT", "Combat ending");
        view.printLine("\n=== COMBAT END ===\n");

        // FLEE SYSTEM DISABLED
        /*
         * if (playerFled || monsterFled) {
         * logger.info("COMBAT", "Combat ended by retreat");
         * return;
         * }
         */

        if (monster.isAlive()) {
            if (!player.isAlive() && !hasAliveCompanion()) {
                logger.warn("COMBAT", "Party defeated");
                view.showError("Your party was defeated!");
                player.kill();
            }
        } else {
            logger.info("COMBAT", "Victory! Player gained " + monster.getExperience() + " XP");
            view.showSuccess("Victory!");

            // OVERKILL XP BONUS SYSTEM
            // Reward stylish victories with high combo counts
            // Combo >= 10: +20% XP bonus
            // Combo >= 15: +35% XP bonus
            // Combo >= 20: +50% XP bonus
            int baseXP = monster.getExperience();
            int bonusXP = 0;
            String bonusText = "";

            if (comboCount >= 20) {
                bonusXP = (int) (baseXP * 0.50);
                bonusText = ConsoleColor.BRIGHT_YELLOW + " [LEGENDARY COMBO! +50% XP]" + ConsoleColor.RESET;
            } else if (comboCount >= 15) {
                bonusXP = (int) (baseXP * 0.35);
                bonusText = ConsoleColor.BRIGHT_YELLOW + " [EPIC COMBO! +35% XP]" + ConsoleColor.RESET;
            } else if (comboCount >= 10) {
                bonusXP = (int) (baseXP * 0.20);
                bonusText = ConsoleColor.BRIGHT_YELLOW + " [GREAT COMBO! +20% XP]" + ConsoleColor.RESET;
            }

            int totalXP = baseXP + bonusXP;
            if (bonusXP > 0) {
                view.printLine(ConsoleColor.BRIGHT_GREEN + "Gained " + totalXP + " XP (" + baseXP + " + " + bonusXP
                        + " bonus)" + bonusText);
            }

            boolean leveledUp = player.gainExperience(totalXP);
            player.addScore(totalXP);

            this.playerLeveledUp = leveledUp;
        }
    }

    private boolean playerLeveledUp = false;

    public boolean didPlayerLevelUp() {
        return playerLeveledUp;
    }

    // PLAYER FLEE ATTEMPT - DISABLED
    /*
     * // Player combat flee attempt
     * private void attemptPlayerFlee() {
     * if (monster.isBoss()) {
     * view.showWarning("You cannot flee from a boss!");
     * view.pressAnyKey();
     * return;
     * }
     * 
     * int playerEscape = (player.getSpeed() * 2) + player.getIntelligence() +
     * (player.getStrength() / 2);
     * int monsterThreat = monster.getDamage() + monster.getDefense() +
     * (monster.getSpeed() / 2);
     * 
     * int successChance = 40 + ((playerEscape - monsterThreat) / 2);
     * successChance = Math.max(20, Math.min(70, successChance));
     * 
     * boolean success = rng.nextInt(100) < successChance;
     * 
     * if (success) {
     * 
     * int scoreLoss = (int) (player.getTotalScore() * 0.05);
     * int healthLoss = (int) (player.getHealth() * 0.10);
     * 
     * if (player.getTotalScore() >= scoreLoss && scoreLoss > 0) {
     * player.addScore(-scoreLoss);
     * view.showSuccess("You successfully fled! Lost " + scoreLoss + " score.");
     * } else if (healthLoss > 0) {
     * player.takeDamage(healthLoss);
     * view.showSuccess("You successfully fled! Lost " + healthLoss + " health.");
     * } else {
     * view.showSuccess("You successfully fled!");
     * }
     * 
     * playerFled = true;
     * monster.takeDamage(monster.getHealth()); // End combat
     * logger.info("COMBAT", "Player fled successfully");
     * } else {
     * // FAILURE: Monster gets one free attack
     * int freeAttack = monster.getDamage() + rng.nextInt(10);
     * player.takeDamage(freeAttack);
     * view.showWarning("Flee failed! " + monster.getName() + " attacks for " +
     * freeAttack + " damage!");
     * logger.info("COMBAT", "Player flee attempt failed");
     * }
     * 
     * view.pressAnyKey();
     * }
     */

    // FLEE SYSTEM DISABLED
    /*
     * // Not used
     * private boolean shouldMonsterFlee() {
     * // Only flee if health is critically low
     * if (monster.getHealth() > monster.getMaxHealth() * 0.20) {
     * return false;
     * }
     * 
     * // Calculate if player is significantly stronger
     * int playerPower = player.getStrength() + player.getSpeed() +
     * (player.getResilience() / 2);
     * int monsterPower = monster.getDamage() + monster.getDefense();
     * 
     * // Flee if player is 1.5x stronger and random chance triggers
     * boolean outmatched = playerPower > monsterPower * 1.5;
     * boolean chanceTrigger = rng.nextInt(100) < 35;
     * 
     * return outmatched && chanceTrigger;
     * }
     */

    // Heal to x cap
    private void healWithCap(int amount) {
        int currentHealth = player.getHealth();
        int maxAllowedHealth = Math.min(combatHealingCap, player.getMaxHealth());

        if (currentHealth >= maxAllowedHealth) {

            return;
        }

        int actualHeal = Math.min(amount, maxAllowedHealth - currentHealth);
        player.heal(actualHeal);
    }
}
