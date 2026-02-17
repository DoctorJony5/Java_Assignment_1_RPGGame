// Jonathan Decondé - 3196362
package game.entities.monsters;

import util.RNG;

/**
 * MonsterAbility - Represents special attacks and actions that monsters can use in combat.
 * Different monster types and rarities have access to different abilities.
 */
public class MonsterAbility {
    private String name;
    private String description;
    private AbilityType type;
    private int staminaCost;
    private int minDamage;
    private int maxDamage;
    private int cooldown;
    private int currentCooldown;

    public enum AbilityType {
        BASIC_ATTACK,
        HEAVY_STRIKE,
        RAPID_STRIKE,
        LIFE_DRAIN,
        POISON_ATTACK,
        BLEED_ATTACK,
        STUNNING_BLOW,
        DEFENSIVE_STANCE,
        ENRAGE, // Poor idea
        SUMMON_ADDS, // To implement sometime for Necromancer Boss
        BOSS_TELEGRAPH, // To rework
        COUNTER_STANCE, // NEW: Reflect damage back
        SPELL_CAST, // NEW: Magical damage type
        MULTI_STRIKE // NEW: Hit twice per turn
    }

    public MonsterAbility(String name, String description, AbilityType type, int staminaCost, int minDamage,
            int maxDamage, int cooldown) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.staminaCost = staminaCost;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.cooldown = cooldown;
        this.currentCooldown = 0;
    } // Constructor, should be at the bottom for reading flow; but this kept having
      // to be redone so it's here.

    // Calculate damage with a minimum and maximum range
    // I use FAR too much RNG in this project
    public int calculateDamage(RNG rng) {
        if (maxDamage <= minDamage) {
            return minDamage;
        }
        return rng.nextInt(minDamage, maxDamage);
    }

    // unused for now
    public boolean isAvailable() {
        return currentCooldown <= 0;
    }

    /**
     * Use the ability, putting it on cooldown
     */
    public void use() {
        currentCooldown = cooldown;
    }

    // Reduce cooldown by 1
    public void tickCooldown() {
        if (currentCooldown > 0) {
            currentCooldown--;
        }
    }

    // Reset
    public void resetCooldown() {
        currentCooldown = 0;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AbilityType getType() {
        return type;
    }

    public int getStaminaCost() {
        return staminaCost;
    }

    public int getMinDamage() {
        return minDamage;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getCurrentCooldown() {
        return currentCooldown;
    }

    // Method factory for abilities
    // These are actually quite simple to setup, though unbalanced / quite boring
    public static MonsterAbility createBasicAttack() {
        return new MonsterAbility("Claw Attack", "A basic melee strike",
                AbilityType.BASIC_ATTACK, 10, 10, 18, 0);
    }

    public static MonsterAbility createHeavyStrike() {
        // Heavy strikes are meant as more of a high risk high reward move
        // They're not as spammable as basic attacks (ideally)
        return new MonsterAbility("Heavy Strike", "A powerful crushing blow",
                AbilityType.HEAVY_STRIKE, 25, 22, 38, 2);
    }

    public static MonsterAbility createRapidStrike() {
        // Rapid strikes are weaker but can hit multiple times - not ideal
        return new MonsterAbility("Rapid Strike", "Multiple quick attacks",
                AbilityType.RAPID_STRIKE, 20, 6, 14, 3);
    }

    public static MonsterAbility createLifeDrain() {
        // Mildly useful life drain ability - not too powerful, rather underpowered though.
        return new MonsterAbility("Life Drain", "Drains life from the target",
                AbilityType.LIFE_DRAIN, 30, 18, 28, 4);
    }

    public static MonsterAbility createPoisonAttack() {

        return new MonsterAbility("Poison Fang", "Injects deadly poison",
                AbilityType.POISON_ATTACK, 20, 12, 20, 3);
    }

    public static MonsterAbility createBleedAttack() {

        return new MonsterAbility("Rending Slash", "Causes severe bleeding",
                AbilityType.BLEED_ATTACK, 20, 14, 22, 3);
    }

    public static MonsterAbility createStunningBlow() {
  
        return new MonsterAbility("Stunning Blow", "A strike that staggers the enemy",
                AbilityType.STUNNING_BLOW, 25, 17, 25, 4);
    }

    public static MonsterAbility createDefensiveStance() {
        return new MonsterAbility("Defensive Stance", "Takes a defensive position",
                AbilityType.DEFENSIVE_STANCE, 15, 0, 0, 2);
    }

    public static MonsterAbility createEnrage() {
        return new MonsterAbility("Enrage", "Enters a berserker rage",
                AbilityType.ENRAGE, 20, 0, 0, 5);
    }

    public static MonsterAbility createBossTelegraph(String moveName) {
        return new MonsterAbility(moveName, "Charges up a devastating attack",
                AbilityType.BOSS_TELEGRAPH, 30, 40, 60, 4);
    }

    public static MonsterAbility createCounterStance() {
        // Reflects some damage back to attacker - tactical defensive move
        return new MonsterAbility("Counter Stance", "Prepares to reflect incoming damage",
                AbilityType.COUNTER_STANCE, 18, 0, 0, 3);
    }

    public static MonsterAbility createSpellCast() {
        // Magical damage - harder to mitigate with armor
        return new MonsterAbility("Arcane Bolt", "Casts magical energy at target",
                AbilityType.SPELL_CAST, 22, 20, 32, 2);
    }

    public static MonsterAbility createMultiStrike() {
        // Hits twice - lower individual damage but higher total
        return new MonsterAbility("Twin Strike", "Attacks with two quick strikes",
                AbilityType.MULTI_STRIKE, 28, 12, 18, 3);
    }

    public static MonsterAbility createStoneCrash() {
        return new MonsterAbility("Stone Crash", "A crushing slam from above",
                AbilityType.HEAVY_STRIKE, 24, 20, 34, 2);
    }

    public static MonsterAbility createVenomSpray() {
        return new MonsterAbility("Venom Spray", "Sprays corrosive venom",
                AbilityType.POISON_ATTACK, 22, 10, 18, 3);
    }

    public static MonsterAbility createShadowLash() {
        return new MonsterAbility("Shadow Lash", "Cuts with a shadowy whip",
                AbilityType.BLEED_ATTACK, 22, 12, 20, 3);
    }

    public static MonsterAbility createArcaneBurst() {
        return new MonsterAbility("Arcane Burst", "Unleashes volatile magic",
                AbilityType.SPELL_CAST, 24, 22, 36, 3);
    }

    public static MonsterAbility createPiercingFlurry() {
        return new MonsterAbility("Piercing Flurry", "A rapid series of strikes",
                AbilityType.RAPID_STRIKE, 22, 8, 16, 2);
    }
}
