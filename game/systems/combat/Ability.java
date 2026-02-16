// Jonathan Decondé - 3196362
package game.systems.combat;

import util.RNG;

// Abilities are used in combat by both players and monsters
public class Ability {
    private String name;
    private String description;
    private int staminaCost;
    private int minDamage;
    private int maxDamage;
    private boolean isDefensive;
    private double damageMultiplier;
    // These ought to be finals, but I don't want to redo everything and make things in capslock

    public Ability(String name, String description, int staminaCost, int minDamage, int maxDamage,
            boolean isDefensive) {
        this.name = name;
        this.description = description;
        this.staminaCost = staminaCost;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.isDefensive = isDefensive;
        this.damageMultiplier = 1.0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getStaminaCost() {
        return staminaCost;
    }

    // Calculate stamina cost with weapon rarity scaling
    // Better weapons reduce stamina costs (easier to wield with practice)
    // Formula: Base Cost × (1.0 - (rarityMult - 1.0) × 0.15)
    public int getScaledStaminaCost(double weaponRarityMult) {
        double reduction = (weaponRarityMult - 1.0) * 0.15; // 15% reduction per rarity tier
        double scaledCost = staminaCost * (1.0 - Math.min(0.6, reduction)); // Cap at 60% reduction
        return Math.max(5, (int) scaledCost); // Minimum 5 SP
    }

    public int getMinDamage() {
        return minDamage;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public boolean isDefensive() {
        return isDefensive;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public int calculateDamage(RNG rng) {
        // Calculate damage within min-max range, handling edge case where min == max
        int range = Math.max(0, maxDamage - minDamage);
        int baseDamage = minDamage + (range == 0 ? 0 : rng.nextInt(range + 1));
        return (int) (baseDamage * damageMultiplier);
    }

    @Override
    public String toString() {
        return String.format("%s (Cost: %d SP) - %s", name, staminaCost, description);
    }
}
