// Jonathan Decondé - 3196362
package game.items;

// Weapon and item traits that provide special effects in combat
public enum ItemTrait {
    // DAMAGE TRAITS
    BLEED("Bleed: Causes bleeding, dealing DoT damage over time"),
    VAMPIRIC("Vampiric: Heals user for portion of damage dealt"),
    EXECUTE("Execute: Extra damage when enemy is weak (< 35% HP)"),
    SWIFT("Swift: Restores stamina on hit, enabling more attacks"),

    // Cursed weapons have inverted mechanics: stronger when user is desperate
    // Below 75% health: +25% damage per 25% missing HP (1-4x multiplier)
    // Above 75% health: Heals enemy by 10% of damage dealt (reverse vampiric!)
    // Strategic choice: Use when desperate vs. avoid at full health
    CURSED("Cursed: Damage scales with desperation, but heals enemy when healthy"),

    // Blessed weapons generate healing aura and provide defensive benefits
    // Grants +5% damage reduction while equipped
    // Heals user for 5% of damage dealt (like mild vampiric)
    // 20% chance to reflect damage back to attacker
    BLESSED("Blessed: Protective aura, damage reflection, mild healing"),

    
    CORRUPTING("Corrupting: Gains power with each consecutive hit");

    public final String description;

    ItemTrait(String description) {
        this.description = description;
    }
}
