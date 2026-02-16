// Jonathan Decondé - 3196362
package game.items;

import game.ElementType;
import game.ItemType;
import game.Rarity;

// basic item.
public class Item {
    private String name;
    private ItemType type;
    private ElementType element;
    private Rarity rarity;
    private int damage;
    private int defense;
    private int healAmount;
    private int value; // There was a store system and whatever, as well as actual NPCs with cool
                       // parsing logic. Gave up on that though.
    private ItemTrait trait;
    private String setTag;

    public Item(String name, ItemType type, ElementType element, Rarity rarity) {
        this.name = name;
        this.type = type;
        this.element = element;
        this.rarity = rarity;
        this.calculateStats();
    }

    public void setTrait(ItemTrait trait) {
        this.trait = trait;
    }

    public void setSetTag(String setTag) {
        this.setTag = setTag;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public void setValue(int value) {
        this.value = value;
    }

    private void calculateStats() {
        double rarityMult = rarity.statMultiplier;

        if (type.isWeapon()) { // fancy stat calculator
            this.damage = (int) (type.baseDamage * rarityMult);
            this.value = (int) (type.baseDamage * 10 * rarityMult);
        } else if (type.isArmor()) {
            this.defense = (int) Math.max(1, type.baseDefense * rarityMult);
            this.value = (int) (type.baseDefense * 15 * rarityMult);
        } else if (type == ItemType.POTION_HEALTH) {
            this.healAmount = (int) (30 * rarityMult);
            this.value = (int) (25 * rarityMult);
        } else if (type == ItemType.POTION_MANA) {
            this.healAmount = (int) (20 * rarityMult);
            this.value = (int) (20 * rarityMult);
        } else if (type == ItemType.POTION_STAMINA) {
            this.healAmount = (int) (15 * rarityMult);
            this.value = (int) (15 * rarityMult);
        } else if (type == ItemType.POTION_XP) {
            this.healAmount = (int) (50 * rarityMult);
            this.value = (int) (100 * rarityMult);
        } else if (type == ItemType.MAP_FRAGMENT) {
            this.value = 75;
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public ItemType getType() {
        return type;
    }

    public ElementType getElement() {
        return element;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getDamage() {
        return damage;
    }

    public int getDefense() {
        return defense;
    }

    public int getHealAmount() {
        return healAmount;
    }

    /**
     * Get the actual heal amount scaled by player intelligence and level.
     * Higher intelligence and level = more effective potions
     * Formula: baseHeal * (1.0 + (intelligence / 100) * (level / 20))
     */
    public int getScaledHealAmount(int playerIntelligence, int playerLevel) {
        double intelligenceBonus = playerIntelligence / 100.0; // 0.0 to 1.0+ based on INT stat
        double levelBonus = playerLevel / 20.0; // 0.0 to 1.0+ based on level
        double multiplier = 1.0 + (intelligenceBonus * levelBonus); // Combines both
        return Math.max(1, (int) (healAmount * multiplier));
    }

    /**
     * Calculate HoT (Heal over Time) values for new potion system
     * Returns [initialHeal, hotTotal, turns]
     * Initial heal scales with intelligence/level
     * HoT duration (turns) scales with intelligence/luck (max 8 turns)
     * HoT total amount is 2-3x the initial heal
     */
    public int[] getPotionHoTValues(int playerIntelligence, int playerLevel, int playerLuck) {
        // Initial heal: base amount scaled by INT + level
        int initialHeal = getScaledHealAmount(playerIntelligence, playerLevel);

        // HoT turns: 3-8 turns based on intelligence and luck
        // Formula: 3 + (INT/20) + (LUCK/25), capped at 8
        int hotTurns = 3 + (playerIntelligence / 20) + (playerLuck / 25);
        hotTurns = Math.max(3, Math.min(8, hotTurns)); // Clamp between 3 and 8

        // HoT total: 2-3x initial heal based on player level
        // Lower levels get closer to 2x, higher levels get closer to 3x
        double multiplier = 2.0 + (Math.min(playerLevel, 30) / 30.0); // 2.0 to 3.0
        int hotTotal = (int) (initialHeal * multiplier);

        return new int[] { initialHeal, hotTotal, hotTurns };
    }

    public int getValue() {
        return value;
    }

    public ItemTrait getTrait() {
        return trait;
    }

    public String getSetTag() {
        return setTag;
    }

    public String getDetailLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" | ").append(rarity.name()).append(" | ").append(type.displayName);
        if (type.isWeapon()) {
            sb.append(" | DMG ").append(damage);
            if (trait != null) {
                sb.append(" | Trait ").append(trait.name());
            }
        } else if (type.isArmor()) {
            sb.append(" | DEF ").append(defense);
            if (setTag != null) {
                sb.append(" | Set ").append(setTag);
            }
        } else if (type.isPotion()) {
            if (type == ItemType.POTION_MANA) {
                sb.append(" | Disabled");
            } else {
                sb.append(" | Heal ").append(healAmount);
            }
        }
        sb.append(" | Value ").append(value).append("g");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s [%s] <%s>", name, type.displayName, rarity.name());
    }

    /**
     * Serialize item to a string for save file persistence
     * Format: name|type|element|rarity|damage|defense|healAmount|value|trait|setTag
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("|");
        sb.append(type.name()).append("|");
        sb.append(element.name()).append("|");
        sb.append(rarity.name()).append("|");
        sb.append(damage).append("|");
        sb.append(defense).append("|");
        sb.append(healAmount).append("|");
        sb.append(value).append("|");
        sb.append(trait != null ? trait.name() : "NONE").append("|");
        sb.append(setTag != null ? setTag : "NONE");
        return sb.toString();
    }

    /**
     * Deserialize item from a saved string
     * Reconstructs item with all original properties intact
     */
    public static Item deserialize(String serialized) {
        String[] parts = serialized.split("\\|", -1); // -1 keeps trailing empty strings
        if (parts.length < 10) {
            return null; // Invalid format
        }

        try {
            String name = parts[0];
            ItemType type = ItemType.valueOf(parts[1]);
            ElementType element = ElementType.valueOf(parts[2]);
            Rarity rarity = Rarity.valueOf(parts[3]);
            int damage = Integer.parseInt(parts[4]);
            int defense = Integer.parseInt(parts[5]);
            int healAmount = Integer.parseInt(parts[6]);
            int value = Integer.parseInt(parts[7]);
            String traitStr = parts[8];
            String setTag = parts[9];

            // Create the item
            Item item = new Item(name, type, element, rarity);

            // Restore all properties
            item.damage = damage;
            item.defense = defense;
            item.healAmount = healAmount;
            item.value = value;
            item.setTag = (setTag.equals("NONE")) ? null : setTag;

            // Restore trait if it exists
            if (!traitStr.equals("NONE")) {
                item.trait = ItemTrait.valueOf(traitStr);
            }

            return item;
        } catch (IllegalArgumentException e) {
            // Invalid enum value or parsing error
            return null;
        }
    }
}
