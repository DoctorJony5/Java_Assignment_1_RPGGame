// Jonathan Decondé - 3196362
package game.entities;

import game.ElementType;
import game.Rarity;
import game.items.Item;
import game.items.LoreItem;
import util.ConsoleColor;
import util.DynArray;
import util.RNG;

// The Player! 
// This is a mess of a class, and manages nearly everything to do with the player
// stats, inventory, equipment, progression, etc.

public class Player {
    private String name; // A LOT of these COULD be FINAL, but as I kept changing things, I left them
                         // mutable.
    private int level; // Current character level (increases with experience)
    private int experience; // Current experience points toward next level
    private int experienceToNextLevel; // XP needed to level up (increases each level)

    // These represent the player's core stats and are used in damage calculations,
    // defense, etc.
    private int health; // Current HP
    private int maxHealth; // Maximum HP (increases with level)
    private int stamina; // Used to activate abilities in combat
    private int maxStamina; // Maximum stamina (increases with level)
    private int strength; // Increases outgoing damage
    private int resilience; // Reduces incoming damage (like armor but internal)
    private int speed; // Determines turn order in combat; higher speed = act first
    private int intelligence; // Unused; there was magic stuff in an earlier build, but I wasn't happy with
                              // it.
    private int luck; // Currently just affects rarity of item drops
    private int unspentStatPoints; // Points allocated in level-up to distribute among stats

    private ElementType elementalAffinity; // Exists, isn't really used - placeholder for future magic system
    private boolean alive; // Whether the player is still alive (false after taking lethal damage)
    private boolean hasChosenToExit; // Whether player has chosen to exit the dungeon

    // Each equipment slot can hold one item that provides bonuses
    private Item equippedWeapon; // Increases damage output
    private Item equippedHelmet; // Armor for head
    private Item equippedChestplate; // Armor for torso
    private Item equippedLeggings; // Armor for legs
    private Item equippedBoots; // Armor for feet
    // Note: Could have been better structured as an enum array

    private boolean hasTorch; // Provides extended vision in dark areas (originally debug only, now a real
                              // feature)

    private double monsterAggressionMult; // Multiplier for monster damage (Tremors event = 1.15x)
    private boolean miasmaActive; // Toxic Miasma: damages player 2 per turn, monsters weakened 15%
    private boolean cursedActive; // Cursed Floor: weapon effectiveness inverted, risky risk-reward
    private int curseLayer; // Stacking curse level (increases curse effect)

    private boolean blessingActive; // Ancient Blessing: +30% first strike, +10% XP reward
    private double blessingPower; // Blessing intensity (0-1.0 scale)
    private boolean auraActive; // Ethereal Aura: +15% XP gain, -10% defense penalty

    // Unimplemented concepts / ideas
    private double trapDensityMult; // Crystalline Tunnels: 1.3x more trap frequency
    private int frostLayers; //
    // TODO: Implement frostLayers effects in movement and combat

    private DynArray<Item> inventory; // Dynamic array holding all items the player has collected
    private static final int INVENTORY_LIMIT = 500; // Arbitrary limit to prevent infinite hoarding
    // initially tried weight-based system, then level-based, now just count-based

    private DynArray<LoreItem> loreCollection; // Story items the player has discovered

    // Potion HoT (Heal over Time) effects - new potion system
    private int healthPotionHoTRemaining; // Remaining HP to be healed over time
    private int healthPotionHoTTurns; // Turns remaining for health HoT
    private int staminaPotionHoTRemaining; // Remaining stamina to be restored over time
    private int staminaPotionHoTTurns; // Turns remaining for stamina HoT

    // These track player actions for achievement system and leaderboards
    private int monstersDefeated;
    private int puzzlesSolved;
    private int mimicsDefeated;
    private int itemsCrafted;
    private int chestsOpened;

    private int floorReached; // Highest floor number reached (used to track progress)
    private long totalScore; // Running score (increases with floor progression and combat success)

    // Constructor for the player upon new game creation
    public Player(String name) {
        this(name, new RNG());
    }

    // Player constructor
    public Player(String name, RNG rng) {
        this.name = name;
        this.level = 1;
        this.experience = 0;
        this.experienceToNextLevel = 100;

        this.maxHealth = 100;
        this.health = maxHealth;
        this.maxStamina = 100;
        this.stamina = maxStamina;

        this.strength = 10;
        this.resilience = 5;
        this.speed = 10;
        this.intelligence = 10;
        this.luck = 5;

        // Randomly assign element affinity at start
        ElementType[] elements = { ElementType.FIRE, ElementType.WATER, ElementType.EARTH,
                ElementType.AIR, ElementType.LIGHT, ElementType.DARK };
        this.elementalAffinity = elements[rng.nextInt(elements.length)];

        this.floorReached = 1;
        this.totalScore = 0;
        this.alive = true;
        this.hasChosenToExit = false;
        this.hasTorch = false; // When debugging, set to true

        this.inventory = new DynArray<>(INVENTORY_LIMIT);
        this.loreCollection = new DynArray<>();
        // I am quite happy with the DynArray
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public int getHealth() {
        return Math.max(0, Math.min(health, maxHealth));
    }

    public int getMaxHealth() {
        return Math.max(1, maxHealth);
    }

    public int getStamina() {
        return Math.max(0, Math.min(stamina, maxStamina));
    }

    public int getMaxStamina() {
        return Math.max(1, maxStamina);
    }

    public int getStrength() {
        return strength;
    }

    public int getResilience() {
        return resilience;
    }

    public int getSpeed() {
        return speed;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getLuck() {
        return luck;
    }

    public ElementType getElementalAffinity() {
        return elementalAffinity;
    }

    public int getFloorReached() {
        return floorReached;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public boolean isAlive() {
        return alive;
    }

    public DynArray<Item> getInventory() {
        return inventory;
    }

    public Item getEquippedWeapon() {
        return equippedWeapon;
    }

    public Item getEquippedHelmet() {
        return equippedHelmet;
    }

    public Item getEquippedChestplate() {
        return equippedChestplate;
    }

    public Item getEquippedLeggings() {
        return equippedLeggings;
    }

    public Item getEquippedBoots() {
        return equippedBoots;
    }

    public boolean hasTorch() {
        return hasTorch;
    }

    public void setTorch(boolean hasTorch) {
        this.hasTorch = hasTorch;
    }

    // ===== HEALTH MANAGEMENT =====

    public void takeDamage(int damage) {
        // Damage reduction formula:
        // 1. First reduce by armor (flat reduction from equipped items)
        // 2. Then apply resilience (percentage reduction: higher resilience = less
        // damage)
        // 3. Ensure minimum 1 damage always gets through (player can't be unkillable)

        int armor = getArmorValue(); // Sum of all equipped armor defense values
        int afterArmor = Math.max(0, damage - armor); // Flat armor reduction

        // Resilience is 0-100, represents a percentage reduction
        // At 50 resilience, take 50% damage. At 100 resilience, take 0% damage (but
        // capped by min 1)
        // This is a little overpowered at high resilience, but unlikely to reach 100
        // Hopefully at least.
        int reducedDamage = (int) (afterArmor * (1.0 - (resilience / 100.0)));
        reducedDamage = Math.max(1, reducedDamage); // Minimum 1 damage always hits

        this.health -= reducedDamage;
        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
        }
    }

    public int getArmorValue() {
        int total = 0;
        if (equippedHelmet != null)
            total += equippedHelmet.getDefense();
        if (equippedChestplate != null)
            total += equippedChestplate.getDefense();
        if (equippedLeggings != null)
            total += equippedLeggings.getDefense();
        if (equippedBoots != null)
            total += equippedBoots.getDefense();
        return Math.max(0, total + getSetBonusDefense());
    }

    public int getTotalDefense() {
        return getArmorValue();
    }

    public int getTotalAttack() {
        int weaponDamage = equippedWeapon != null ? equippedWeapon.getDamage() : 0;
        return Math.max(0, strength + weaponDamage);
    }

    private int getSetBonusDefense() {
        // Scales with rarity and player level
        // Formula: base_bonus * rarity_multiplier * level_scaling
        // This ensures set bonuses remain relevant throughout progression
        //
        // Base Bonus (by piece count):
        // - 2 pieces: Base +2 DEF
        // - 3 pieces: Base +4 DEF
        // - 4 pieces: Base +6 DEF (full set bonus)
        //
        // Rarity Multipliers:
        // - COMMON: 1.0x (no bonus)
        // - UNCOMMON: 1.5x
        // - RARE: 2.0x
        // - MYTHICAL: 2.5x
        // - LEGENDARY: 3.0x
        // - DEBUG: 5.0x
        //
        // Level Scaling: +10% per 10 levels (caps at +50% at level 50)
        // Examples:
        // - 2pc Common @ Lv1: 2 * 1.0 * 1.0 = 2 DEF
        // - 4pc Legendary @ Lv1: 6 * 3.0 * 1.0 = 18 DEF
        // - 4pc Legendary @ Lv50: 6 * 3.0 * 1.5 = 27 DEF
        // - 4pc Rare @ Lv25: 6 * 2.0 * 1.25 = 15 DEF

        String[] tags = new String[4];
        Item[] pieces = new Item[4];
        int idx = 0;

        if (equippedHelmet != null && equippedHelmet.getSetTag() != null) {
            tags[idx] = equippedHelmet.getSetTag();
            pieces[idx] = equippedHelmet;
            idx++;
        }
        if (equippedChestplate != null && equippedChestplate.getSetTag() != null) {
            tags[idx] = equippedChestplate.getSetTag();
            pieces[idx] = equippedChestplate;
            idx++;
        }
        if (equippedLeggings != null && equippedLeggings.getSetTag() != null) {
            tags[idx] = equippedLeggings.getSetTag();
            pieces[idx] = equippedLeggings;
            idx++;
        }
        if (equippedBoots != null && equippedBoots.getSetTag() != null) {
            tags[idx] = equippedBoots.getSetTag();
            pieces[idx] = equippedBoots;
            idx++;
        }

        int bestBonus = 0;

        // Find the set with the most pieces and calculate its bonus
        for (int i = 0; i < idx; i++) {
            int count = 1;
            if (tags[i] == null)
                continue;

            // Count matching set pieces
            for (int j = i + 1; j < idx; j++) {
                if (tags[i].equals(tags[j])) {
                    count++;
                }
            }

            // Skip if less than 2 pieces (no set bonus)
            if (count < 2)
                continue;

            // Calculate base bonus by piece count
            int baseBonus = switch (count) {
                case 2 -> 2; // 2-piece set
                case 3 -> 4; // 3-piece set
                default -> 6; // 4-piece set (full set)
            };

            // Get average rarity of this set's pieces for multiplier calculation
            double totalRarityMult = 0;
            int setCount = 0;
            for (int k = 0; k < idx; k++) {
                if (tags[i].equals(tags[k])) {
                    totalRarityMult += getRarityMultiplier(pieces[k].getRarity());
                    setCount++;
                }
            }
            double avgRarityMult = totalRarityMult / setCount;

            // Calculate level scaling (10% per 5 levels, caps at 75%)
            double levelScaling = 1.0 + Math.min(0.75, (level / 5) * 0.1);

            // Final set bonus calculation
            int setBonus = (int) (baseBonus * avgRarityMult * levelScaling);

            // Track best bonus across all potential sets
            bestBonus = Math.max(bestBonus, setBonus);
        }

        return bestBonus;
    }

    // Helper method to get rarity multiplier for set bonus scaling
    private double getRarityMultiplier(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.0;
            case MYTHICAL -> 2.5;
            case LEGENDARY -> 3.0;
            case DEBUG -> 5.0;
        };
    }

    public void heal(int amount) {
        this.health = Math.min(health + amount, maxHealth);
    }

    public void useStamina(int amount) {
        this.stamina = Math.max(0, stamina - amount);
    }

    public void restoreStamina(int amount) {
        this.stamina = Math.min(stamina + amount, maxStamina);
    }

    public void restoreAllStamina() {
        this.stamina = maxStamina;
    }

    // Potion HoT (Heal over Time) system
    /**
     * Apply a potion's HoT effect
     * 
     * @param initialHeal Immediate heal amount
     * @param hotTotal    Total amount to heal over time
     * @param turns       Number of turns to spread the HoT
     * @param isHealth    True for health potion, false for stamina
     */
    public void applyPotionHoT(int initialHeal, int hotTotal, int turns, boolean isHealth) {
        int safeTurns = Math.max(0, turns);
        int safeHotTotal = Math.max(0, hotTotal);

        if (isHealth) {
            heal(initialHeal); // Immediate effect
            healthPotionHoTRemaining = safeHotTotal;
            healthPotionHoTTurns = safeTurns;
        } else {
            restoreStamina(initialHeal); // Immediate effect
            staminaPotionHoTRemaining = safeHotTotal;
            staminaPotionHoTTurns = safeTurns;
        }
    }

    /**
     * Tick potion HoT effects (call each turn in combat or periodically during
     * exploration)
     * 
     * @return String describing what healed, or empty string if nothing
     */
    public String tickPotionHoT() {
        StringBuilder result = new StringBuilder();

        // Health HoT tick
        if (healthPotionHoTTurns > 0 && healthPotionHoTRemaining > 0) {
            // Heal diminishes each turn (70% to 20% of per-turn amount)
            double efficiency = 0.70 - (0.50 * ((double) (8 - healthPotionHoTTurns) / 8.0));
            efficiency = Math.max(0.20, efficiency); // Minimum 20%

            int healPerTurn = (int) Math.ceil(healthPotionHoTRemaining / (double) healthPotionHoTTurns);
            int actualHeal = (int) (healPerTurn * efficiency);
            actualHeal = Math.min(actualHeal, healthPotionHoTRemaining); // Don't overheal

            heal(actualHeal);
            healthPotionHoTRemaining -= actualHeal;
            healthPotionHoTTurns--;

            if (actualHeal > 0) {
                result.append(ConsoleColor.BRIGHT_GREEN).append("⚕ +").append(actualHeal)
                        .append(" HP (potion)").append(ConsoleColor.RESET);
            }
        }

        // Stamina HoT tick
        if (staminaPotionHoTTurns > 0 && staminaPotionHoTRemaining > 0) {
            double efficiency = 0.70 - (0.50 * ((double) (8 - staminaPotionHoTTurns) / 8.0));
            efficiency = Math.max(0.20, efficiency);

            int restorePerTurn = (int) Math.ceil(staminaPotionHoTRemaining / (double) staminaPotionHoTTurns);
            int actualRestore = (int) (restorePerTurn * efficiency);
            actualRestore = Math.min(actualRestore, staminaPotionHoTRemaining);

            restoreStamina(actualRestore);
            staminaPotionHoTRemaining -= actualRestore;
            staminaPotionHoTTurns--;

            if (actualRestore > 0) {
                if (result.length() > 0)
                    result.append(" | ");
                result.append(ConsoleColor.BRIGHT_CYAN).append("⚕ +").append(actualRestore)
                        .append(" ST (potion)").append(ConsoleColor.RESET);
            }
        }

        return result.toString();
    }

    public boolean hasActivePotionHoT() {
        return (healthPotionHoTTurns > 0 && healthPotionHoTRemaining > 0) ||
                (staminaPotionHoTTurns > 0 && staminaPotionHoTRemaining > 0);
    }

    public String getPotionHoTStatus() {
        if (!hasActivePotionHoT())
            return "";

        StringBuilder status = new StringBuilder("Active Potion Effects: ");
        if (healthPotionHoTTurns > 0 && healthPotionHoTRemaining > 0) {
            status.append("HP regen (").append(healthPotionHoTTurns).append(" turns, ~")
                    .append(healthPotionHoTRemaining).append(" HP) ");
        }
        if (staminaPotionHoTTurns > 0 && staminaPotionHoTRemaining > 0) {
            status.append("ST regen (").append(staminaPotionHoTTurns).append(" turns, ~")
                    .append(staminaPotionHoTRemaining).append(" ST)");
        }
        return status.toString();
    }

    // Validated stat setters
    public void setHealth(int value) {
        this.health = Math.max(0, Math.min(value, maxHealth));
    }

    public void setMaxHealth(int value) {
        this.maxHealth = Math.max(1, value);
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public void setStamina(int value) {
        this.stamina = Math.max(0, Math.min(value, maxStamina));
    }

    public void setMaxStamina(int value) {
        this.maxStamina = Math.max(1, value);
        if (stamina > maxStamina) {
            stamina = maxStamina;
        }
    }

    public void setExperience(int value) {
        this.experience = Math.max(0, value);
    }

    public void setLevel(int value) {
        this.level = Math.max(1, value);
    }

    public void setStrength(int value) {
        this.strength = Math.max(0, Math.min(value, 100));
    }

    public void setResilience(int value) {
        this.resilience = Math.max(0, Math.min(value, 100));
    }

    // Convenience method for UI display and calculations
    public int getStaminaPercentage() {
        return (int) ((stamina / (double) maxStamina) * 100);
    }

    // bug: sometimes players can somehow get negative experience - no idea how /
    // why
    public boolean gainExperience(int amount) {
        this.experience += amount;
        boolean leveled = false;
        while (experience >= experienceToNextLevel) {
            levelUp();
            leveled = true;
        }
        return leveled;
    }

    private void levelUp() {
        this.level++;
        this.experience -= experienceToNextLevel;

        // Experience requirements grow exponentially with each level
        // Formula: 100 * (1.1 ^ (level - 1))
        // This prevents over-leveling while still rewarding deep dungeon runs
        // TODO: Consider scaling differently on higher difficulties (harder leveling =
        // more achievement)

        this.experienceToNextLevel = (int) (100 * Math.pow(1.1, level - 1));

        // Health and stamina increases per level
        // +10 HP / +5 stamina per level compounds with equipment bonus
        this.maxHealth += 10;
        this.health = maxHealth;
        this.maxStamina += 5;
        this.stamina = maxStamina;

        // Stat points for player customization
        // +3 points per level allows minimal character building
        this.unspentStatPoints += 3; // todo: balance - should this scale with level?
    }

    // badly designed, but oh well
    public boolean spendStatPoint(String statName) {
        // Allocate unspent stat points to character attributes
        // Supports both full names (strength, speed) and abbreviations (str, spd)
        // This flexibility allows keyboard shortcuts or UI buttons to work uniformly
        // Each point spent increases one stat by +1 permanently
        //
        // Stat effects:
        // - Strength: +1 increases weapon damage (especially important for low-level
        // weapons)
        // - Resilience: +1 increases damage reduction percentage (0-100% cap)
        // - Speed: Used in future mechanics (currently placeholder)
        // - Intelligence: Used in future mechanics (currently placeholder)
        // - Luck: Affects critical hits and rare drops (currently minor impact)
        //
        // TODO: Speed and Intelligence are unimplemented are underused / make no real
        // sense
        // TODO: Luck stat doesn't scale properly with game difficulty / get used well

        if (unspentStatPoints <= 0) {
            return false;
        }

        switch (statName.toLowerCase()) {
            case "strength":
            case "str":
                // Balance needed.
                if (strength < 75) {
                    strength++;
                    unspentStatPoints--;
                    return true;
                }
                return false;
            case "resilience":
            case "res":
                // Prevent very high resilience from making player nearly invincible
                if (resilience < 80) {
                    resilience++;
                    unspentStatPoints--;
                    return true;
                }
                return false;
            case "speed":
            case "spd":
                // Cap speed
                if (speed < 50) {
                    speed++;
                    unspentStatPoints--;
                    return true;
                }
                return false;
            case "intelligence":
            case "int":
                // Cap intelligence, even if unused.
                if (intelligence < 40) {
                    intelligence++;
                    unspentStatPoints--;
                    return true;
                }
                return false;
            case "luck":
            case "lck":
                // Cap luck, even if underused
                if (luck < 30) {
                    luck++;
                    unspentStatPoints--;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    // Inventory Management
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (inventory.size() >= INVENTORY_LIMIT) {
            return false;
        }
        inventory.add(item);
        return true;
    }

    // AUTO-EQUIP: Automatically equips better items when picked up
    // Compares rarity tiers and auto-equips if new item is better
    // Returns true if item was auto-equipped, false if not
    public boolean tryAutoEquip(Item item) {
        // Check if item should be auto-equipped
        if (item.getType().isWeapon()) {
            if (equippedWeapon == null || item.getRarity().tier > equippedWeapon.getRarity().tier) {
                if (equippedWeapon != null) {
                    inventory.add(equippedWeapon); // Put old weapon in inventory
                }
                equipWeapon(item);
                return true;
            }
        } else if (item.getType().isArmor()) {
            Item currentArmor = null;
            switch (item.getType()) {
                case ARMOR_HELMET:
                    currentArmor = equippedHelmet;
                    break;
                case ARMOR_CHESTPLATE:
                    currentArmor = equippedChestplate;
                    break;
                case ARMOR_LEGGINGS:
                    currentArmor = equippedLeggings;
                    break;
                case ARMOR_BOOTS:
                    currentArmor = equippedBoots;
                    break;
                default:
                    break;
            }

            // Auto-equip if no armor in slot or new armor is better (higher rarity)
            if (currentArmor == null || item.getRarity().tier > currentArmor.getRarity().tier) {
                if (currentArmor != null) {
                    inventory.add(currentArmor); // Put old armor in inventory
                }
                equipArmor(item);
                return true;
            }
        }

        return false; // No auto-equip performed
    }

    public boolean removeItem(Item item) {
        return inventory.remove(item);
    }

    public Item removeItemAt(int index) {
        return inventory.remove(index);
    }

    // Equipment Management
    public void equipWeapon(Item weapon) {
        if (weapon.getType().isWeapon()) {
            this.equippedWeapon = weapon;
        }
    }

    public void equipArmor(Item armor) {
        if (!armor.getType().isArmor()) {
            return;
        }
        switch (armor.getType()) {
            case ARMOR_HELMET:
                this.equippedHelmet = armor;
                break;
            case ARMOR_CHESTPLATE:
                this.equippedChestplate = armor;
                break;
            case ARMOR_LEGGINGS:
                this.equippedLeggings = armor;
                break;
            case ARMOR_BOOTS:
                this.equippedBoots = armor;
                break;
            default:
                break;
        }
    }

    // Score Management
    public void addScore(int points) {
        this.totalScore += points;
    }

    public void addLoreItem(LoreItem lore) {
        if (loreCollection == null) {
            loreCollection = new DynArray<>();
        }
        loreCollection.add(lore);
    }

    public DynArray<LoreItem> getLoreCollection() {
        if (loreCollection == null) {
            loreCollection = new DynArray<>();
        }
        return loreCollection;
    }

    public void incrementMonstersDefeated() {
        monstersDefeated++;
    }

    public void incrementPuzzlesSolved() {
        puzzlesSolved++;
    }

    public void incrementMimicsDefeated() {
        mimicsDefeated++;
    }

    public void incrementItemsCrafted() {
        itemsCrafted++;
    }

    public void incrementChestsOpened() {
        chestsOpened++;
    }

    public int getMonstersDefeated() {
        return monstersDefeated;
    }

    public int getPuzzlesSolved() {
        return puzzlesSolved;
    }

    public int getMimicsDefeated() {
        return mimicsDefeated;
    }

    public int getItemsCrafted() {
        return itemsCrafted;
    }

    public int getChestsOpened() {
        return chestsOpened;
    }

    public int getUnspentStatPoints() {
        return unspentStatPoints;
    }

    public void setUnspentStatPoints(int points) {
        this.unspentStatPoints = points;
    }

    public void setFloorReached(int floor) {
        if (floor > this.floorReached) {
            this.floorReached = floor;
            this.totalScore += floor * 100; // Bonus for reaching new floors
        }
    }

    public boolean hasChosenExit() {
        return hasChosenToExit;
    }

    public void setChosenExit(boolean chosen) {
        this.hasChosenToExit = chosen;
    }

    // Not used currently, future mechanic idea
    public void setMonsterAggression(double multiplier) {
        this.monsterAggressionMult = multiplier;
    }

    public double getMonsterAggression() {
        return monsterAggressionMult;
    }

    public void setMiasmaActive(boolean active) {
        this.miasmaActive = active;
    }

    public boolean isMiasmaActive() {
        return miasmaActive;
    }

    public void setAuraActive(boolean active) {
        this.auraActive = active;
    }

    public boolean isAuraActive() {
        return auraActive;
    }

    public void setBlessing(boolean active) {
        this.blessingActive = active;
    }

    public boolean hasBlessing() {
        return blessingActive;
    }

    public void setTrapDensity(double multiplier) {
        this.trapDensityMult = multiplier;
    }

    public double getTrapDensity() {
        return trapDensityMult;
    }

    public void setCursed(boolean active) {
        this.cursedActive = active;
    }

    public boolean isCursed() {
        return cursedActive;
    }

    public void setCurseLayer(int layer) {
        this.curseLayer = layer;
    }

    public int getCurseLayer() {
        return curseLayer;
    }

    public void setBlessingPower(double power) {
        this.blessingPower = Math.max(0, Math.min(1.0, power)); // Clamp 0-1
    }

    public double getBlessingPower() {
        return blessingPower;
    }

    public void setFrostLayers(int layers) {
        this.frostLayers = layers;
    }

    public int getFrostLayers() {
        return frostLayers;
    }

    // Save/Load
    public void loadFromSaveData(int level, int exp, int hp, int maxHp, int stam, int maxStam,
            int str, int res, int spd, int intel, int lck, int unspent,
            int floor, long score,
            int monstersKilled, int puzzles, int mimics, int crafted, int chests) {
        this.level = level;
        this.experience = exp;
        this.health = hp;
        this.maxHealth = maxHp;
        this.stamina = stam;
        this.maxStamina = maxStam;
        this.strength = str;
        this.resilience = res;
        this.speed = spd;
        this.intelligence = intel;
        this.luck = lck;
        this.unspentStatPoints = unspent;
        this.floorReached = floor;
        this.totalScore = score;
        this.monstersDefeated = monstersKilled;
        this.puzzlesSolved = puzzles;
        this.mimicsDefeated = mimics;
        this.itemsCrafted = crafted;
        this.chestsOpened = chests;
    }

    public void kill() {
        this.alive = false;
        this.health = 0;
    } // Kill the player

    @Override
    public String toString() {
        return String.format("%s | Level %d | HP: %d/%d | Floor: %d | Score: %d",
                name, level, health, maxHealth, floorReached, totalScore);
    }
}
