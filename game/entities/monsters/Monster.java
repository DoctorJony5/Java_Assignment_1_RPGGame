// Jonathan Decondé - 3196362
package game.entities.monsters;

import game.ElementType;
import game.Rarity;
import util.DynArray;
import util.RNG;

/**
 * Monster - Represents enemies in the dungeon.
 * Each monster has type, weakness, stats, and AI behavior.
 * This was designed to be easy to expand with new monsters and stuff
 * And I had plans for smarter AI too. Half implemented ish kinda (not really)
 * 
 * 
 * 
 * Future Plans:
 * Redesign entire Monster and Player class to inherit from a common "Entity"
 * class
 * This would reduce code duplication and allow shared logic for combat, stats,
 * etc.
 * Implement status effects system (poison, stun, burn) that can be applied to
 * both players and monsters
 * Better AI decision with behaviour trees / state machines, allowing loot
 * drops, more combat variety / options
 * More modular ability system
 * Better balancing & scaling with player progression
 * 
 */
public class Monster {
    private String name;
    private ElementType type;
    private ElementType weakness;
    private Rarity rarity;

    // a LOT of these things COULD be made into FINALs or etc,
    // But this all needed to be flexible because I kept changing things,
    // and if time allowed I'd redesign the entire combat & monster system anyways.

    // Difficulty scaling
    private double difficultyMultiplier = 1.0;
    private double speedMultiplier = 1.0; // Stat modifier based on monster type
    private double strengthMultiplier = 1.0; // Stat modifier based on monster type
    private boolean hasFirstStrikeDamageBonus = false; // Will deal extra damage on first attack

    private int level;
    private int health;
    private int maxHealth;
    private int stamina;
    private int maxStamina;
    private int damage;
    private int defense;
    private int experience;

    private boolean alive;
    private static RNG rng = new RNG(); // Shared RNG for all monsters; not smart. Too late to change.

    // Special abilities
    private DynArray<MonsterAbility> abilities;
    private MonsterAbility nextTelegraphedAbility; // For boss telegraphing
    private boolean isEnraged;
    private boolean isDefending;

    public Monster(String name, ElementType type, ElementType weakness, Rarity rarity, int level) {
        this.name = name;
        this.type = type;
        this.weakness = weakness;
        this.rarity = rarity;
        this.level = level;

        calculateStats();
        this.alive = true;
        this.abilities = new DynArray<>();
        this.nextTelegraphedAbility = null;
        this.isEnraged = false;
        this.isDefending = false;
        initializeAbilities();
    }

    private void calculateStats() {
        double rarityMult = rarity.statMultiplier;
        double levelMult = 1.0 + (level * 0.1);

        // Defense now scales with floor progression
        // This prevents player defense bonuses from trivializing later floors
        // Defense formula: base * difficulty * (1 + floor_bonus)
        // This ensures monsters remain dangerous as player gets stronger gear
        double defenseFloorBonus = 1.0 + (Math.max(0, level - 1) * 0.05);

        this.maxHealth = (int) (50 * levelMult * rarityMult * difficultyMultiplier);
        this.health = maxHealth;
        this.maxStamina = (int) (80 * rarityMult * difficultyMultiplier);
        this.stamina = maxStamina;
        // Monster damage now scales more aggressively with level
        // This prevents player from becoming too strong relative to enemies
        // Added 1.15x multiplier to monster damage scaling
        // Apply strength multiplier based on monster type
        this.damage = (int) (15 * levelMult * rarityMult * difficultyMultiplier * 1.15 * strengthMultiplier);
        this.defense = (int) (5 * levelMult * rarityMult * difficultyMultiplier * defenseFloorBonus);
        this.experience = (int) (50 * level * rarityMult * difficultyMultiplier);
    } // Funny numbers

    public void takeDamage(int damage) {
        // Damage reduction now caps at 75% to prevent monsters from being invincible
        // Formula: damage * (1 - min(defense/100, 0.75))
        // This means even heavily armored monsters take at least 25% of incoming damage
        // Prevents situations where defense values become too high to overcome
        double defenseRatio = Math.min(defense / 100.0, 0.75);
        int reducedDamage = (int) (damage * (1.0 - defenseRatio));
        reducedDamage = Math.max(1, reducedDamage); // Ensure at least 1 damage is taken.
        this.health -= reducedDamage;
        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
        }
    } // This is a poorly designed system but whatever

    /**
     * Calculate and return damage breakdown information for display
     * Returns: [baseDamage, reducedDamage, defenseValue, defensePercentage]
     */
    public int[] getDamageBreakdown(int damage) {
        double defenseRatio = Math.min(defense / 100.0, 0.75);
        int reducedDamage = (int) (damage * (1.0 - defenseRatio));
        reducedDamage = Math.max(1, reducedDamage);
        int defenseReduction = damage - reducedDamage;
        int defensePercent = damage > 0 ? (int) ((defenseReduction * 100.0) / damage) : 0;
        return new int[] { damage, reducedDamage, defenseReduction, defensePercent };
    }

    public void useStamina(int amount) {
        this.stamina = Math.max(0, stamina - amount);
    } // Stamina can't go below 0

    public void heal(int amount) {
        if (amount <= 0)
            return;
        this.health = Math.min(maxHealth, health + amount);
        this.alive = health > 0;
    }

    public void restoreStamina() {
        this.stamina = maxStamina;
    }

    /**
     * Initialize abilities based on rarity and type
     */
    private void initializeAbilities() {
        // All monsters get basic attack
        abilities.add(MonsterAbility.createBasicAttack());

        // Uncommon monsters get one special ability based on element
        if (rarity == Rarity.UNCOMMON) {
            switch (type) {
                case FIRE -> abilities.add(MonsterAbility.createArcaneBurst());
                case WATER -> abilities.add(MonsterAbility.createCounterStance());
                case EARTH -> abilities.add(MonsterAbility.createStoneCrash());
                case AIR -> abilities.add(MonsterAbility.createPiercingFlurry());
                case DARK -> abilities.add(MonsterAbility.createShadowLash());
                case LIGHT -> abilities.add(MonsterAbility.createSpellCast());
                case MAGIC -> abilities.add(MonsterAbility.createArcaneBurst());
                default -> abilities.add(MonsterAbility.createHeavyStrike());
            }
        }

        // Rare monsters get element ability + heavy strike + one tactical ability
        if (rarity == Rarity.RARE) {
            // Element-based ability
            switch (type) {
                case FIRE -> abilities.add(MonsterAbility.createArcaneBurst());
                case WATER -> abilities.add(MonsterAbility.createLifeDrain());
                case EARTH -> abilities.add(MonsterAbility.createStoneCrash());
                case AIR -> abilities.add(MonsterAbility.createMultiStrike());
                case DARK -> abilities.add(MonsterAbility.createVenomSpray());
                case LIGHT -> abilities.add(MonsterAbility.createSpellCast());
                case MAGIC -> abilities.add(MonsterAbility.createArcaneBurst());
                default -> abilities.add(MonsterAbility.createHeavyStrike());
            }
            // Additional tactical ability
            int roll = rng.nextInt(4);
            switch (roll) {
                case 0 -> abilities.add(MonsterAbility.createStunningBlow());
                case 1 -> abilities.add(MonsterAbility.createDefensiveStance());
                case 2 -> abilities.add(MonsterAbility.createCounterStance());
                case 3 -> abilities.add(MonsterAbility.createHeavyStrike());
            }
        }

        // Legendary/Mythical bosses get full arsenal with telegraphing
        if (rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
            abilities.add(MonsterAbility.createHeavyStrike());
            abilities.add(MonsterAbility.createMultiStrike());
            abilities.add(MonsterAbility.createBossTelegraph("Devastating Strike"));
            abilities.add(MonsterAbility.createEnrage());
            abilities.add(MonsterAbility.createCounterStance());
        }

        // UPDATED: Uncommon/Rare monsters now have more tactical variety
        // This makes fights more interesting and less "just tank and deal damage"
        // Different monsters have different tactics which the player must adapt to
    }

    /**
     * Choose which ability to use based on "smart" "AI" "logic"
     * 
     * Considers player stats and combat state
     * 
     * @return The chosen ability, or null if resting
     */
    public MonsterAbility chooseAbility() {
        // Monster AI decision making: choose which ability to use this turn
        // Uses a priority-based system: telegraphed > heal > defend > stun > random
        // This creates predictable but not boring behavior - monsters don't act
        // "randomly" (they still sort of do)
        //
        // Telegraphed abilities are special: they're announced one turn in advance
        // This gives the player warning before big damage. Ideally there would be more
        // interesting related features or systems
        // but this is, as a lot of things, a poor implementation of the idea.

        // If telegraphed ability is ready, use it
        // (Telegraphed ability was announced last turn, player had warning)
        if (nextTelegraphedAbility != null) {
            MonsterAbility toUse = nextTelegraphedAbility;
            nextTelegraphedAbility = null;
            return toUse;
        }

        // Low stamina - rest instead
        // Stamina threshold of 20 means monsters won't use expensive abilities at low
        // stamina
        // This prevents monsters from wasting resources and allows player to predict
        // rest turns
        if (stamina < 20) {
            return null;
        }

        // Build list of available abilities (not on cooldown, enough stamina)
        // This filters out abilities that are cooling down or too expensive to cast
        DynArray<MonsterAbility> available = new DynArray<>();
        for (int i = 0; i < abilities.size(); i++) {
            MonsterAbility ability = abilities.get(i);
            if (ability.isAvailable() && stamina >= ability.getStaminaCost()) {
                available.add(ability);
            }
        } // this should really be at the start of the turn, but too late to change

        // No abilities available, use basic attack
        if (available.size() == 0) {
            return abilities.get(0); // Basic attack
        }

        // Heal when low health - very basic AI prioritization
        // If below 40% health and a healing ability exists, use it immediately
        // This makes monsters less passive and increases fight duration/difficulty
        if (health < maxHealth * 0.4) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.LIFE_DRAIN) {
                    return ability; // Heal immediately
                }
            }
        }

        // Defend when very low health
        // If below 30% health and defensive ability exists, prioritize defense over
        // offense
        // This makes monsters less likely to die in one turn when already wounded
        if (health < maxHealth * 0.3) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.DEFENSIVE_STANCE) {
                    return ability;
                }
            }
        }

        // Counter stance when threatened but not critical
        if (health < maxHealth * 0.5) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.COUNTER_STANCE) {
                    if (rng.nextInt(100) < 50) {
                        return ability;
                    }
                }
            }
        }

        // Try to stun player when possible
        // 40% chance to use stun ability when available (not 100%, to keep it
        // unpredictable)
        // Stun is high-value (prevents player turn) so lower chance prevents it being
        // oppressive
        for (int i = 0; i < available.size(); i++) {
            MonsterAbility ability = available.get(i);
            if (ability.getType() == MonsterAbility.AbilityType.STUNNING_BLOW) {
                if (rng.nextInt(100) < 40) { // todo: balance - should this be higher on harder difficulties?
                    return ability;
                }
            }
        }

        // Spell casts: useful mid-fight burst
        for (int i = 0; i < available.size(); i++) {
            MonsterAbility ability = available.get(i);
            if (ability.getType() == MonsterAbility.AbilityType.SPELL_CAST) {
                if (rng.nextInt(100) < 35) {
                    return ability;
                }
            }
        }

        // Multi-strike: pressure the player when healthy
        if (health > maxHealth * 0.4) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.MULTI_STRIKE) {
                    if (rng.nextInt(100) < 35) {
                        return ability;
                    }
                }
            }
        }

        // Proof I don't understand game design or fun
        if (available.size() > 1) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.ENRAGE) {
                    if (health > maxHealth * 0.5 && rng.nextInt(100) < 30) {
                        return ability;
                    }
                }
            }
        }

        // Boss telegraphing - announce next big attack when health drops
        if (rarity == Rarity.LEGENDARY && health < maxHealth * 0.6) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.BOSS_TELEGRAPH) {
                    nextTelegraphedAbility = ability;
                    return abilities.get(0); // Use basic attack while telegraphing
                }
            }
        }
        // This started as an idea by a friend, but this is a VERY VERY poor
        // implementation of it all
        // There's no real telegraphing system in place, just a one-off ability that
        // bosses get
        // It was supposed to add more depth to boss fights, but it doesn't really do
        // that well - boring / bad addition

        // DoT abilities stuff
        if (health > maxHealth * 0.7) {
            for (int i = 0; i < available.size(); i++) {
                MonsterAbility ability = available.get(i);
                if (ability.getType() == MonsterAbility.AbilityType.POISON_ATTACK ||
                        ability.getType() == MonsterAbility.AbilityType.BLEED_ATTACK) {
                    if (rng.nextInt(100) < 35) {
                        return ability;
                    }
                }
            }
        }

        // Go for high damage when possible
        MonsterAbility best = available.get(0);
        for (int i = 1; i < available.size(); i++) {
            MonsterAbility ability = available.get(i);
            if (ability.getMaxDamage() > best.getMaxDamage()) {
                best = ability;
            }
        }

        return best;
        // This whole method is just a mess of poorly thought out logic and bad design
    }

    // Poor implementation of cooldown ticking
    // I thought that this could make things more fun or even challenging
    // Doesn't do anything !
    public void tickAbilityCooldowns() {
        for (int i = 0; i < abilities.size(); i++) {
            abilities.get(i).tickCooldown();
        }
    }

    public int getAIAction() {
        if (stamina < 20) {
            return 2; // Rest action
        }
        if (health < maxHealth / 3) {
            return 1; // Defensive action
        }
        return 0; // Aggressive action
    }

    // Getters
    public String getName() {
        return name;
    }

    public ElementType getType() {
        return type;
    }

    public ElementType getWeakness() {
        return weakness;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getLevel() {
        return level;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getStamina() {
        return stamina;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public int getDamage() {
        return damage;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return (int) ((10 + level) * Math.max(1.0, difficultyMultiplier * 0.9) * speedMultiplier);
    } // Speed scales with level and monster type multiplier

    public int getExperience() {
        return experience;
    }

    public boolean isAlive() {
        return alive;
    }

    public DynArray<MonsterAbility> getAbilities() {
        return abilities;
    }

    public MonsterAbility getNextTelegraphedAbility() {
        return nextTelegraphedAbility;
    }

    public boolean isEnraged() {
        return isEnraged;
    }

    public void setEnraged(boolean enraged) {
        this.isEnraged = enraged;
    }

    public boolean isDefending() {
        return isDefending;
    }

    public boolean isBoss() {
        return rarity.ordinal() >= Rarity.LEGENDARY.ordinal() || name.toLowerCase().contains("boss");
    }

    public void applyDifficulty(double multiplier) {
        this.difficultyMultiplier = Math.max(0.5, multiplier);
        calculateStats();
    }

    public void setDefending(boolean defending) {
        this.isDefending = defending;
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
        calculateStats();
    }

    public void setStrengthMultiplier(double multiplier) {
        this.strengthMultiplier = multiplier;
        calculateStats();
    }

    public void setHasFirstStrikeDamageBonus(boolean hasBonus) {
        this.hasFirstStrikeDamageBonus = hasBonus;
    }

    public boolean getHasFirstStrikeDamageBonus() {
        return hasFirstStrikeDamageBonus;
    }

    @Override
    public String toString() {
        return String.format("%s [Lvl %d] HP: %d/%d | %s", name, level, health, maxHealth, rarity.name());
    }
}
