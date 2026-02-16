// Jonathan Decondé - 3196362
package game.items;

// Just a big list of name parts for generating item names.
// Separated to keep things tidy.
// Ought to be data driven but whatever.

public final class ItemNameLists {

        public static final String[] COMMON_PREFIX = {
                        "Rusty", "Worn", "Old", "Cracked", "Dull", "Bent", "Broken",
                        "Weathered", "Chipped", "Blunt", "Jagged", "Crude", "Rough", "Simple",
                        "Dented", "Battered", "Shabby", "Faded"
        };

        public static final String[] UNCOMMON_PREFIX = {
                        "Sturdy", "Balanced", "Refined", "Tempered", "Polished", "Well-kept", "Reliable",
                        "Keen", "Solid", "Sharp", "Crafted", "Practiced", "Fine", "Noble",
                        "Durable", "Strong", "Steadfast", "Trusty", "Capable", "Focused", "Precise",
                        "Spirited", "Vigilant", "Dependable", "Seasoned", "Masterwork"
        };

        public static final String[] RARE_PREFIX = {
                        "Enchanted", "Runed", "Gleaming", "Battleforged", "Arcane", "Mystic", "Blessed",
                        "Sacred", "Ancient", "Ethereal", "Shimmering", "Prismatic", "Luminous", "Fabled",
                        "Magnificent", "Potent", "Hallowed", "Wondrous"
        };

        public static final String[] MYTHICAL_PREFIX = {
                        "Mythic", "Dragonforged", "Celestial", "Stormbound", "Moonlit", "Infernal", "Twilight",
                        "Radiant", "Star-Kissed", "Void-Born", "Spectral", "Otherworldly", "Transcendent", "Apex",
                        "Astral", "Eldritch", "Sorcerous"
        };

        public static final String[] LEGENDARY_PREFIX = {
                        "Legendary", "Godforged", "Eternal", "Worldbreaker", "Doombringer", "Fate-Woven", "Timeless",
                        "Infinite", "Undying", "Supreme", "Divine", "Omnipotent", "Paramount", "Sovereign",
                        "Unfathomable", "Empyrean", "Aeon-Cursed"
        };

        public static final String[] COMMON_SUFFIX = {
                        "of Splinters", "of the Rat", "of Dust", "of Rust", "of Mice", "of Decay",
                        "of Drudgery", "of Common Folk", "of the Wanderer", "of the Pauper",
                        "of Weary Hands", "of Forgotten Times", "of Ashpits", "of Lingering Doubt"
        };

        public static final String[] UNCOMMON_SUFFIX = {
                        "of Steady Hands", "of the Scout", "of Focus", "of the Watch", "of Valor",
                        "of the Guard", "of Precision", "of Cunning", "of Oath", "of the Veteran",
                        "of Proven Skill", "of True Aim", "of the Defender", "of Sharp Reflexes",
                        "of the Sentinel", "of Honest Labor", "of Reliable Fortune"
        };

        public static final String[] RARE_SUFFIX = {
                        "of Sparks", "of Fury", "of the Grove", "of Emberlight", "of Starfire",
                        "of the Archmage", "of Rending", "of the Dragon", "of Tempests", "of Twilight",
                        "of Wrath", "of Ancient Power", "of Scorching Flame", "of Winter's Bite",
                        "of Thunderous Might", "of the Sage", "of Wild Nature", "of Mystic Knowledge"
        };

        public static final String[] MYTHICAL_SUFFIX = {
                        "of the Dragon", "of Dawn", "of Midnight", "of the Tempest", "of the Cosmos",
                        "of Cataclysm", "of the Abyss", "of Ascension", "of Ruin", "of Creation",
                        "of Eternity", "of the Void", "of the Phoenix", "of Celestial Fire",
                        "of Broken Skies"
        };

        public static final String[] LEGENDARY_SUFFIX = {
                        "of Eternity", "of the First Flame", "of Kings", "of the Void",
                        "of Apocalypse", "of Eons", "of Damnation", "of Salvation",
                        "of Dominion", "of Transcendence", "of Fate", "of Divine Judgment",
                        "of Primordial Power", "of the Infinite", "of the Forgotten God", "of Everlasting Glory"
        };

        public static final String[] SWORD_NOUN = {
                        "Blade", "Edge", "Longsword", "Cutlass", "Saber",
                        "Claymore", "Falchion", "Greatsword", "Scimitar", "Rapier",
                        "Broadsword", "Shortsword", "Katana", "Gladius", "Arming Sword"
        };

        public static final String[] AXE_NOUN = {
                        "Axe", "Crescent", "Cleaver", "Hatchet", "Splitter",
                        "Maul", "Broadaxe", "Poleaxe", "Tomahawk", "Labrys",
                        "Francisca", "War Axe", "Battleaxe"
        };

        public static final String[] DAGGER_NOUN = {
                        "Dagger", "Knife", "Sting", "Dirk", "Shiv",
                        "Poniard", "Stiletto", "Fang", "Poignard", "Kris",
                        "Katar", "Tanto", "Misericorde"
        };

        public static final String[] STAFF_NOUN = {
                        "Staff", "Rod", "Channeler", "Focus", "Spire",
                        "Wand", "Crook", "Stave", "Scepter", "Conduit",
                        "Aetherstave", "Runestave", "Moonstave"
        };

        public static final String[] BOW_NOUN = {
                        "Bow", "Recurve", "Longbow", "Shortbow", "String",
                        "Crossbow", "Yew Bow", "Composite", "Archer's Pride",
                        "Ballista", "Greatbow", "War Bow"
        };

        public static final String[] ARMOR_HELMET_NOUN = {
                        "Helm", "Cap", "Visor", "Mask", "Circlet",
                        "Crown", "Cowl", "Coif", "Sallet", "Armet",
                        "Barbute", "Great Helm", "Basinet", "Morion", "Diadem"
        };

        public static final String[] ARMOR_CHEST_NOUN = {
                        "Chest", "Plate", "Cuirass", "Hauberk", "Brigandine",
                        "Corselet", "Haubergeon", "Jerkin", "Breastplate", "Gorget",
                        "Scale Mail", "Lamellar", "Klibanion", "Plastron", "Aegis"
        };

        public static final String[] ARMOR_GAUNTLET_NOUN = {
                        "Grips", "Gauntlets", "Gloves", "Wraps", "Bracers",
                        "Mittens", "Fists", "Pauldrons", "Cuffs", "Armblades",
                        "Fistguards", "Vambraces", "Couters", "Handplates", "Armbands"
        };

        public static final String[] ARMOR_LEG_NOUN = {
                        "Greaves", "Leggings", "Tassets", "Cuisses", "Chausses",
                        "Leg Plates", "Poleyn", "Sabaton", "Hose", "Thighpiece",
                        "Faulds", "Tuille", "Jambeau", "Soleret", "Gamboised Hose"
        };

        public static final String[] ARMOR_BOOT_NOUN = {
                        "Boots", "Sabatons", "Footpads", "Shoes", "Sollerets",
                        "Footguards", "Treads", "Plateshoes", "Walkers", "Buskins",
                        "Poulaines", "Turnshoes", "Caligae", "Armored Boots", "Steel Treads"
        };

        public static final String[] SET_TAGS = {
                        "Steel", "Shadow", "Ember", "Frost", "Storm",
                        "Iron", "Silver", "Bronze", "Gold", "Obsidian",
                        "Adamant", "Mithril", "Orichalcum", "Titanium", "Runedstone",
                        "Dragonscale", "Void", "Solar", "Lunar", "Inferno",
                        "Glacial", "Tempest", "Earthen", "Azure", "Crimson", "Onyx"
        };
}
