// Jonathan Decondé - 3196362
package game;

// Bad idea, bad impementation
public enum ElementType {
    FIRE,
    WATER,
    EARTH,
    AIR,
    LIGHT,
    DARK,
    PHYSICAL,
    MAGIC,
    NEUTRAL;

    public boolean isEffectiveAgainst(ElementType other) {
        switch (this) {
            case FIRE:
                return other == AIR || other == EARTH;
            case WATER:
                return other == FIRE || other == EARTH;
            case EARTH:
                return other == WATER || other == AIR;
            case AIR:
                return other == WATER || other == LIGHT;
            case LIGHT:
                return other == DARK;
            case DARK:
                return other == LIGHT;
            case MAGIC:
                return other == PHYSICAL;
            case PHYSICAL:
                return other == MAGIC;
            default:
                return false;
        }
    }

    public double getDamageMultiplier(ElementType monsterType) {
        if (isEffectiveAgainst(monsterType)) {
            return 1.2;
        }
        if (monsterType.isEffectiveAgainst(this)) {
            return 0.9; 
        }
        return 1.0; 
    }
}
