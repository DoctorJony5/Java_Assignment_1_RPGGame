package game;

import util.ConsoleColor;

public enum Rarity {
    COMMON(0, 1.0),
    UNCOMMON(1, 1.3),
    RARE(2, 1.7),
    MYTHICAL(3, 2.2),
    LEGENDARY(4, 3.0),
    DEBUG(5, 5.0);

    public final int tier;
    public final double statMultiplier;

    Rarity(int tier, double statMultiplier) {
        this.tier = tier;
        this.statMultiplier = statMultiplier;
    }

    /**
     * Get rarity tier based on random roll value (0-100 range)
     * 
     * Rarity Distribution:
     * - DEBUG: 0.001% (roll < 0.001)
     * - LEGENDARY: 0.099% (0.001 <= roll < 0.1)
     * - MYTHICAL: 0.4% (0.1 <= roll < 0.5)
     * - RARE: 2.5% (0.5 <= roll < 3.0)
     * - UNCOMMON: 12% (3 <= roll < 15)
     * - COMMON: 85% (roll >= 15)
     * 
     * @param roll Random value from 0-100 (typically from rng.nextInt(100))
     * @return Appropriate rarity tier based on probability distribution
     */
    public static Rarity getRarityByChance(double roll) {
        if (roll < 0.001)
            return DEBUG;
        if (roll < 0.1) // FIXED: was 0.9 (90% chance - completely broken)
            return LEGENDARY;
        if (roll < 0.5) // FIXED: was 5 (outside 0-100 range)
            return MYTHICAL;
        if (roll < 3.0) // FIXED: was 15 (was RARE range, now is RARE)
            return RARE;
        if (roll < 15) // FIXED: was 40 (now correct UNCOMMON range)
            return UNCOMMON;
        return COMMON;
    }

    // colours
    public String getColoredName() {
        return switch (this) {
            case COMMON -> ConsoleColor.WHITE + this.name() + ConsoleColor.RESET;
            case UNCOMMON -> ConsoleColor.BRIGHT_GREEN + this.name() + ConsoleColor.RESET;
            case RARE -> ConsoleColor.BRIGHT_BLUE + this.name() + ConsoleColor.RESET;
            case MYTHICAL -> ConsoleColor.BRIGHT_MAGENTA + this.name() + ConsoleColor.RESET;
            case LEGENDARY -> ConsoleColor.BRIGHT_YELLOW + this.name() + ConsoleColor.RESET;
            case DEBUG -> ConsoleColor.BRIGHT_RED + this.name() + ConsoleColor.RESET;
        };
    }
}
