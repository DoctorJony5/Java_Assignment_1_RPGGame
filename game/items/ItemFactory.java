// Jonathan Decondé - 3196362
package game.items;

import game.Difficulty;
import game.ElementType;
import game.ItemType;
import game.Rarity;
import util.RNG;

// A generic "factory" to create items
// The code is quite similar to what I would do in Godot, but with a few (significant) changes
public final class ItemFactory {
    private ItemFactory() {
    }

    public static Item randomWeapon(RNG rng, Rarity rarity) {
        ItemType type = pickWeaponType(rng);
        String name = buildName(rng, rarity, type.displayName, pickWeaponNoun(rng, type));
        Item item = new Item(name, type, ElementType.NEUTRAL, rarity);
        item.setTrait(pickTrait(rng));
        return item;
    }

    public static Item randomArmor(RNG rng, Rarity rarity) {
        ItemType type = pickArmorType(rng);
        String name = buildName(rng, rarity, type.displayName, pickArmorNoun(rng, type));
        Item item = new Item(name, type, ElementType.NEUTRAL, rarity);
        item.setSetTag(pickSetTag(rng));
        return item;
    }

    public static Item randomPotion(RNG rng) {
        // Mana disabled: only generate Health and Stamina potions
        ItemType[] potions = { ItemType.POTION_HEALTH, ItemType.POTION_STAMINA };
        ItemType type = potions[rng.nextInt(potions.length)];
        Rarity rarity = Rarity.COMMON;
        String name = type.displayName;
        return new Item(name, type, ElementType.NEUTRAL, rarity);
    }

    public static Item createBossKey() {
        return new Item("Dungeon Boss Room Key", ItemType.BOSS_KEY, ElementType.NEUTRAL, Rarity.LEGENDARY);
    } // Poorly thought out idea

    // Boss loot, kinda unique
    public static Item generateBossLoot(RNG rng, int floorNumber) {
        // Generate unique boss loot that alternates between weapon and armor rewards
        //
        // Alternation logic: (floorNumber / 5) % 2
        //
        // TODO: Could use floorNumber to scale item quality (floor 50+ = extra rare traits)

        boolean isWeapon = (floorNumber / 5) % 2 == 0;

        if (isWeapon) {
            ItemType type = pickWeaponType(rng);
            String[] bossTitles = { "Dread", "Cursed", "Ancient", "Ethereal", "Infernal", "Divine" };
            String title = pick(rng, bossTitles);
            String name = title + " " + pickWeaponNoun(rng, type);
            Item item = new Item(name, type, ElementType.NEUTRAL, Rarity.LEGENDARY);
            item.setTrait(pickTrait(rng));
            return item;
        } else {
            ItemType type = pickArmorType(rng);
            String[] bossTitles = { "Dragon Scale", "Enchanted", "Void", "Celestial", "Cursed", "Ancient" };
            String title = pick(rng, bossTitles);
            String name = title + " " + pickArmorNoun(rng, type);
            Item item = new Item(name, type, ElementType.NEUTRAL, Rarity.LEGENDARY);
            item.setSetTag(pickSetTag(rng));
            return item;
        }
    }

    public static Rarity rollRarity(RNG rng, Difficulty difficulty, int floorNumber, int playerLevel) {
        // Item rarity probability scaled by dungeon progression
        // Formula: roll between 0-100, then subtract bonuses (lower roll = rarer item)
        // Bonuses increase with difficulty, depth, and player level
        //
        double roll = rng.nextDouble() * 100;
        double difficultyShift = difficulty.level * 2.0;
        double depthBonus = floorNumber * 0.5; // 0.5% per floor
        double levelBonus = playerLevel * 0.3; // 0.3% per level
        roll = Math.max(0, roll - difficultyShift - depthBonus - levelBonus);
        return Rarity.getRarityByChance(roll);
    }

    // My friends has trouble survivng the first floor so this serves as a small boost.
    // Trivializes early game though
    public static void giveStarterGear(Difficulty difficulty, RNG rng, java.util.function.Consumer<Item> onItem) {
        Rarity weaponRarity = switch (difficulty) {
            case TUTORIAL, EASY -> Rarity.COMMON;
            case NORMAL -> Rarity.UNCOMMON;
            case HARD -> Rarity.UNCOMMON;
            case NIGHTMARE -> Rarity.RARE;
            case HELL -> Rarity.RARE;
        };
        Rarity armorRarity = (weaponRarity == Rarity.RARE) ? Rarity.UNCOMMON : Rarity.COMMON;

        onItem.accept(randomWeapon(rng, weaponRarity));
        onItem.accept(randomArmor(rng, armorRarity));
    }

    private static ItemType pickWeaponType(RNG rng) {
        ItemType[] weapons = {
                ItemType.WEAPON_SWORD,
                ItemType.WEAPON_AXE,
                ItemType.WEAPON_DAGGER,
                ItemType.WEAPON_STAFF,
                ItemType.WEAPON_BOW
        };
        return weapons[rng.nextInt(weapons.length)];
    }

    private static ItemType pickArmorType(RNG rng) {
        ItemType[] armor = {
                ItemType.ARMOR_HELMET,
                ItemType.ARMOR_CHESTPLATE,
                ItemType.ARMOR_LEGGINGS,
                ItemType.ARMOR_BOOTS
        };
        return armor[rng.nextInt(armor.length)];
    }

    private static String buildName(RNG rng, Rarity rarity, String base, String noun) {
        String prefix = switch (rarity) {
            case COMMON -> pick(rng, ItemNameLists.COMMON_PREFIX);
            case UNCOMMON -> pick(rng, ItemNameLists.UNCOMMON_PREFIX);
            case RARE -> pick(rng, ItemNameLists.RARE_PREFIX);
            case MYTHICAL -> pick(rng, ItemNameLists.MYTHICAL_PREFIX);
            case LEGENDARY, DEBUG -> pick(rng, ItemNameLists.LEGENDARY_PREFIX);
        };
        String suffix = switch (rarity) {
            case COMMON -> pick(rng, ItemNameLists.COMMON_SUFFIX);
            case UNCOMMON -> pick(rng, ItemNameLists.UNCOMMON_SUFFIX);
            case RARE -> pick(rng, ItemNameLists.RARE_SUFFIX);
            case MYTHICAL -> pick(rng, ItemNameLists.MYTHICAL_SUFFIX);
            case LEGENDARY, DEBUG -> pick(rng, ItemNameLists.LEGENDARY_SUFFIX);
        };
        return prefix + " " + noun + " " + suffix;
    }

    // Names and etc are defined in ItemNameLists
    // Could be data driven but that was overkill for this project (as if it isn't already)
    private static String pickWeaponNoun(RNG rng, ItemType type) {
        return switch (type) {
            case WEAPON_SWORD -> pick(rng, ItemNameLists.SWORD_NOUN);
            case WEAPON_AXE -> pick(rng, ItemNameLists.AXE_NOUN);
            case WEAPON_DAGGER -> pick(rng, ItemNameLists.DAGGER_NOUN);
            case WEAPON_STAFF -> pick(rng, ItemNameLists.STAFF_NOUN);
            case WEAPON_BOW -> pick(rng, ItemNameLists.BOW_NOUN);
            default -> type.displayName;
        };
    }

    private static String pickArmorNoun(RNG rng, ItemType type) {
        return switch (type) {
            case ARMOR_HELMET -> pick(rng, ItemNameLists.ARMOR_HELMET_NOUN);
            case ARMOR_CHESTPLATE -> pick(rng, ItemNameLists.ARMOR_CHEST_NOUN);
            case ARMOR_LEGGINGS -> pick(rng, ItemNameLists.ARMOR_LEG_NOUN);
            case ARMOR_BOOTS -> pick(rng, ItemNameLists.ARMOR_BOOT_NOUN);
            default -> type.displayName;
        };
    }

    private static ItemTrait pickTrait(RNG rng) {
        ItemTrait[] traits = ItemTrait.values();
        return traits[rng.nextInt(traits.length)];
    }

    private static String pickSetTag(RNG rng) {
        return pick(rng, ItemNameLists.SET_TAGS);
    }

    private static String pick(RNG rng, String[] options) {
        return options[rng.nextInt(options.length)];
    }
}
