// Jonathan Decondé - 3196362
package game;

// item types
public enum ItemType {
    WEAPON_SWORD("Sword", 20, 1.0, 0),
    WEAPON_AXE("Axe", 25, 0.9, 0),
    WEAPON_DAGGER("Dagger", 12, 1.2, 0),
    WEAPON_STAFF("Staff", 18, 1.1, 0),
    WEAPON_BOW("Bow", 16, 1.15, 0),

    // REVAMPED: Simplified armor to 4 slots (removed gauntlets)
    ARMOR_HELMET("Helmet", 0, 1.0, 4),
    ARMOR_CHESTPLATE("Chestplate", 0, 1.0, 10),
    ARMOR_LEGGINGS("Leggings", 0, 1.0, 7),
    ARMOR_BOOTS("Boots", 0, 1.0, 3),

    POTION_HEALTH("Health Potion", 0, 1.0, 0),
    POTION_MANA("Mana Potion", 0, 1.0, 0),
    POTION_STAMINA("Stamina Potion", 0, 1.0, 0),
    POTION_XP("XP Potion", 0, 1.0, 0),

    TORCH("Torch", 0, 1.0, 0),
    MAP_FRAGMENT("Map Fragment", 0, 1.0, 0),
    KEY("Key", 0, 1.0, 0),
    BOSS_KEY("Dungeon Boss Key", 0, 1.0, 0),
    QUEST_ITEM("Quest Item", 0, 1.0, 0);

    public final String displayName;
    public final int baseDamage;
    public final double speedMultiplier;
    public final int baseDefense;

    ItemType(String displayName, int baseDamage, double speedMultiplier, int baseDefense) {
        this.displayName = displayName;
        this.baseDamage = baseDamage;
        this.speedMultiplier = speedMultiplier;
        this.baseDefense = baseDefense;
    }

    public boolean isWeapon() {
        return this.name().startsWith("WEAPON_");
    }

    public boolean isArmor() {
        return this.name().startsWith("ARMOR_");
    }

    public boolean isPotion() {
        return this.name().startsWith("POTION_");
    }

    public boolean isEquippable() {
        return isWeapon() || isArmor();
    }
}
