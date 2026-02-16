// Jonathan Decondé - 3196362
package game.items;

import game.ElementType;
import game.ItemType;
import game.Rarity;
import util.RNG;

// Some stupid items for testing.
public final class DEBUG_ItemFactory {
    private DEBUG_ItemFactory() {
    }

    public static Item deathNote() {
        Item item = new Item("Death Note", ItemType.WEAPON_SWORD, ElementType.PHYSICAL, Rarity.DEBUG);
        item.setDamage(1000);
        item.setValue(99999);
        return item;
    }

    public static Item holyWaterPistol() {
        Item item = new Item("Holy Water Pistol", ItemType.WEAPON_BOW, ElementType.LIGHT, Rarity.DEBUG);
        item.setDamage(500);
        item.setValue(50000);
        return item;
    } // I've been througb several different ideas for combat, one of which was more
      // movement / position based
      // But this, though somewhat more interesting than this, was harder to implement
      // and teach to the player
      // and a tutorial was out of the question for scope reasons - this item should
      // still work though, even if no longer interesting.

    public static Item netheriteHelm() {
        Item item = new Item("Netherite Helm", ItemType.ARMOR_HELMET, ElementType.PHYSICAL, Rarity.DEBUG);
        item.setDefense(1000);
        item.setSetTag("Netherite");
        item.setValue(50000);
        return item;
    }

    public static Item netheriteChestplate() {
        Item item = new Item("Netherite Chestplate", ItemType.ARMOR_CHESTPLATE, ElementType.PHYSICAL, Rarity.DEBUG);
        item.setDefense(1000);
        item.setSetTag("Netherite");
        item.setValue(50000);
        return item;
    }

    public static Item netheriteLeggings() {
        Item item = new Item("Netherite Leggings", ItemType.ARMOR_LEGGINGS, ElementType.PHYSICAL, Rarity.DEBUG);
        item.setDefense(1000);
        item.setSetTag("Netherite");
        item.setValue(50000);
        return item;
    }

    public static Item netheriteBoots() {
        Item item = new Item("Netherite Boots", ItemType.ARMOR_BOOTS, ElementType.PHYSICAL, Rarity.DEBUG);
        item.setDefense(1000);
        item.setSetTag("Netherite");
        item.setValue(50000);
        return item;
    }

    public static Item getRandomDebugItem(RNG rng) {
        int choice = rng.nextInt(6);
        return switch (choice) {
            case 0 -> deathNote();
            case 1 -> holyWaterPistol();
            case 2 -> netheriteHelm();
            case 3 -> netheriteChestplate();
            case 4 -> netheriteLeggings();
            case 5 -> netheriteBoots();
            default -> deathNote();
        };
    } // ignore
}
