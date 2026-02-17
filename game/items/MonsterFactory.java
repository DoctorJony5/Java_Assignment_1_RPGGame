// Jonathan Decondé - 3196362
package game.items;

import game.Difficulty;
import game.ElementType;
import game.Rarity;
import game.entities.monsters.Monster;
import util.DynArray;
import util.RNG;

/**
 * Monster "Factory" v3
 * 
 * Redesigned to be more data-driven / template-based.
 * 
 * - MonsterTemplate: A reusable monster archetypes - configs
 * - MonsterConfig: Immutable configuration for level/difficulty scaling
 * - Factory Methods: Clean, type-safe creation API
 */

public class MonsterFactory {

    public static class MonsterConfig {
        public final Difficulty difficulty;
        public final int levelBonus;
        public final boolean allowElite;
        public final double statMultiplier;

        public MonsterConfig(Difficulty difficulty, int levelBonus, boolean allowElite, double statMultiplier) {
            this.difficulty = difficulty;
            this.levelBonus = levelBonus;
            this.allowElite = allowElite;
            this.statMultiplier = statMultiplier;
        }

        // Standard configuration
        public static MonsterConfig fromDifficulty(Difficulty difficulty) {
            return new MonsterConfig(difficulty, 0, true, 1.0);
        }

        // Boss-specific Config
        public static MonsterConfig forBoss(Difficulty difficulty) {
            int levelBonus = switch (difficulty) {
                case TUTORIAL -> 0;
                case EASY -> 1;
                case NORMAL -> 2;
                case HARD -> 3;
                case NIGHTMARE -> 4;
                case HELL -> 5;
            };
            return new MonsterConfig(difficulty, levelBonus, false, difficulty.enemyStatMultiplier * 1.25);
        } // Gives bosses a xx% stat boost (subject to change!)
    }

    // Monster template def
    public static class MonsterTemplate {
        private final String baseName;
        private final ElementType element;
        private final ElementType weakness;
        private final Rarity baseRarity;
        private final String flavorText; // For future tooltip/description system
        private final double speedMultiplier; // Speed stat modifier (1.0 = baseline)
        private final double strengthMultiplier; // Strength stat modifier (1.0 = baseline)

        public MonsterTemplate(String baseName, ElementType element, ElementType weakness,
                Rarity baseRarity, String flavorText, double speedMultiplier, double strengthMultiplier) {
            this.baseName = baseName;
            this.element = element;
            this.weakness = weakness;
            this.baseRarity = baseRarity;
            this.flavorText = flavorText;
            this.speedMultiplier = speedMultiplier;
            this.strengthMultiplier = strengthMultiplier;
        }

        public String getBaseName() {
            return baseName;
        }

        public ElementType getElement() {
            return element;
        }

        public ElementType getWeakness() {
            return weakness;
        }

        public Rarity getBaseRarity() {
            return baseRarity;
        }

        public String getFlavorText() {
            return flavorText;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }

        public double getStrengthMultiplier() {
            return strengthMultiplier;
        }
    }

    // Common enemy templates
    // Speed/Strength multipliers: 1.0 = baseline, >1.0 = stronger/faster, <1.0 = weaker/slower
    public static final MonsterTemplate GOBLIN = new MonsterTemplate(
            "Goblin", ElementType.DARK, ElementType.LIGHT, Rarity.COMMON,
            "Small, green-skinned creatures that travel in packs.", 1.0, 0.9);

    public static final MonsterTemplate RAT = new MonsterTemplate(
            "Rat", ElementType.DARK, ElementType.FIRE, Rarity.COMMON,
            "Disease-ridden vermin that infest the dungeon's dark corners.", 1.2, 0.7); // Fast but weak

    public static final MonsterTemplate SPIDER = new MonsterTemplate(
            "Spider", ElementType.DARK, ElementType.FIRE, Rarity.COMMON,
            "Eight-legged horrors that lurk in the shadows.", 1.3, 0.8); // Very fast, decent damage

    public static final MonsterTemplate SKELETON = new MonsterTemplate(
            "Skeleton", ElementType.DARK, ElementType.LIGHT, Rarity.COMMON,
            "Reanimated bones held together by dark magic.", 0.9, 1.0); // Slower but steady

    public static final MonsterTemplate TROLL = new MonsterTemplate(
            "Troll", ElementType.EARTH, ElementType.FIRE, Rarity.UNCOMMON,
            "Massive humanoids with incredible regeneration abilities.", 0.7, 1.4); // Slow but very strong

    public static final MonsterTemplate WEREWOLF = new MonsterTemplate(
            "Werewolf", ElementType.DARK, ElementType.LIGHT, Rarity.UNCOMMON,
            "Cursed beings torn between human and beast.", 1.4, 1.3); // Fast AND strong

    public static final MonsterTemplate ZOMBIE = new MonsterTemplate(
            "Zombie", ElementType.DARK, ElementType.LIGHT, Rarity.UNCOMMON,
            "Undead abominations that hunger for flesh.", 0.8, 1.1); // Slow but tough

    public static final MonsterTemplate MINOTAUR = new MonsterTemplate(
            "Minotaur", ElementType.EARTH, ElementType.LIGHT, Rarity.LEGENDARY,
            "A legendary beast, half-man and half-bull, guardian of ancient labyrinths.", 1.1, 1.5); // Balanced but
                                                                                                     // powerful

    public static final MonsterTemplate LICH = new MonsterTemplate(
            "Lich", ElementType.DARK, ElementType.LIGHT, Rarity.LEGENDARY,
            "An immortal spellcaster who has transcended death through forbidden magic.", 1.2, 1.3); // Fast caster

    public static final MonsterTemplate HYDRA = new MonsterTemplate(
            "Hydra", ElementType.WATER, ElementType.FIRE, Rarity.LEGENDARY,
            "A multi-headed serpent that grows two heads for each one severed.", 0.9, 1.6); // Slow but devastating

    public static final MonsterTemplate WARLOCK = new MonsterTemplate(
            "Warlock", ElementType.MAGIC, ElementType.LIGHT, Rarity.LEGENDARY,
            "A master of dark arts who has bargained with otherworldly entities.", 1.3, 1.4); // Fast and deadly

    public static final MonsterTemplate MIMIC = new MonsterTemplate(
            "Mimic", ElementType.DARK, ElementType.LIGHT, Rarity.RARE,
            "A shapeshifting predator that disguises itself as treasure chests.", 1.1, 1.2); // Slightly enhanced

    public static final MonsterTemplate TREASURE_GUARDIAN = new MonsterTemplate(
            "Treasure Guardian", ElementType.EARTH, ElementType.MAGIC, Rarity.UNCOMMON,
            "A magical construct bound to protect valuable artifacts.", 0.9, 1.2); // Sturdy protector

    public static final MonsterTemplate PUZZLE_GUARDIAN = new MonsterTemplate(
            "Puzzle Guardian", ElementType.MAGIC, ElementType.DARK, Rarity.RARE,
            "An ancient sentinel that punishes those who fail its tests.", 1.0, 1.3); // Balanced challenger

    // NEW MONSTER TEMPLATES - adds more variety to encounters
    public static final MonsterTemplate BANDIT = new MonsterTemplate(
            "Bandit", ElementType.DARK, ElementType.LIGHT, Rarity.COMMON,
            "A criminal outlaw who relies on speed and cunning.", 1.5, 0.95); // Very fast, weak

    public static final MonsterTemplate GARGOYLE = new MonsterTemplate(
            "Gargoyle", ElementType.EARTH, ElementType.WATER, Rarity.UNCOMMON,
            "A stone creature that can reflect incoming damage.", 0.6, 1.3); // Slow, defensive, strong

    public static final MonsterTemplate SHADOW_BEAST = new MonsterTemplate(
            "Shadow Beast", ElementType.DARK, ElementType.LIGHT, Rarity.UNCOMMON,
            "A creature born from darkness itself, quick and aggressive.", 1.6, 1.2); // Very fast and strong

    public static final MonsterTemplate NECROMANCER = new MonsterTemplate(
            "Necromancer", ElementType.DARK, ElementType.LIGHT, Rarity.RARE,
            "A death-wielding mage who commands the power of the undead.", 1.1, 1.4); // Tactical caster

    public static final MonsterTemplate DRAGON = new MonsterTemplate(
            "Dragon", ElementType.FIRE, ElementType.WATER, Rarity.LEGENDARY,
            "An ancient archwyrm with scales harder than steel and breath of flame.", 1.0, 1.7); // Ultimate threat

        public static final MonsterTemplate CULTIST = new MonsterTemplate(
            "Cultist", ElementType.MAGIC, ElementType.LIGHT, Rarity.COMMON,
            "A zealous fanatic empowered by dark rites.", 1.1, 1.0); // Balanced

        public static final MonsterTemplate SLIME = new MonsterTemplate(
            "Slime", ElementType.WATER, ElementType.FIRE, Rarity.COMMON,
            "A gelatinous creature that splits and reforms.", 0.9, 0.9); // Slow but steady

        public static final MonsterTemplate SCORPION = new MonsterTemplate(
            "Scorpion", ElementType.EARTH, ElementType.WATER, Rarity.COMMON,
            "A venomous predator with a deadly stinger.", 1.2, 1.0); // Fast and balanced

        public static final MonsterTemplate WRAITH = new MonsterTemplate(
            "Wraith", ElementType.DARK, ElementType.LIGHT, Rarity.UNCOMMON,
            "A spectral assassin that drains warmth from the living.", 1.5, 1.1); // Fast and evasive

        public static final MonsterTemplate STONE_BEETLE = new MonsterTemplate(
            "Stone Beetle", ElementType.EARTH, ElementType.FIRE, Rarity.UNCOMMON,
            "A plated insect that shrugs off blows.", 0.7, 1.2); // Defensive bruiser

        public static final MonsterTemplate STORM_DRAKE = new MonsterTemplate(
            "Storm Drake", ElementType.AIR, ElementType.EARTH, Rarity.UNCOMMON,
            "A lesser drake crackling with storm energy.", 1.4, 1.2); // Fast and strong

        public static final MonsterTemplate VAMPIRE = new MonsterTemplate(
            "Vampire", ElementType.DARK, ElementType.LIGHT, Rarity.RARE,
            "A noble predator that thrives on blood.", 1.2, 1.4); // Life drain specialist

        public static final MonsterTemplate BASILISK = new MonsterTemplate(
            "Basilisk", ElementType.EARTH, ElementType.FIRE, Rarity.RARE,
            "A stone-gazing reptile with crushing strength.", 0.9, 1.6); // Heavy hitter

        public static final MonsterTemplate CHRONOMANCER = new MonsterTemplate(
            "Chronomancer", ElementType.MAGIC, ElementType.DARK, Rarity.RARE,
            "A time-bending mage that warps the flow of battle.", 1.3, 1.1); // Tactical caster

        public static final MonsterTemplate PHOENIX = new MonsterTemplate(
            "Phoenix", ElementType.FIRE, ElementType.WATER, Rarity.LEGENDARY,
            "A legendary bird reborn in flame.", 1.2, 1.6); // Fast boss

    // Pools
        private static final MonsterTemplate[] COMMON_TEMPLATES = { GOBLIN, RAT, SPIDER, SKELETON, BANDIT, CULTIST, SLIME,
            SCORPION };
        private static final MonsterTemplate[] UNCOMMON_TEMPLATES = { TROLL, WEREWOLF, ZOMBIE, GARGOYLE, SHADOW_BEAST,
            WRAITH, STONE_BEETLE, STORM_DRAKE };
        private static final MonsterTemplate[] RARE_TEMPLATES = { MIMIC, TREASURE_GUARDIAN, PUZZLE_GUARDIAN, NECROMANCER,
            VAMPIRE, BASILISK, CHRONOMANCER };
        private static final MonsterTemplate[] BOSS_TEMPLATES = { MINOTAUR, LICH, HYDRA, WARLOCK, DRAGON, PHOENIX };

    // Level scaling logic; calculate maximum allowable monster level; prevents
    // over-scaling. Ideally

    private static int calculateMaxLevel(int playerLevel, int floorNumber, Difficulty difficulty) {
        int baseCap = playerLevel + floorNumber;

        int difficultyBonus = switch (difficulty) {
            case TUTORIAL -> 2;
            case EASY -> 2;
            case NORMAL -> 3;
            case HARD -> 4;
            case NIGHTMARE -> 5;
            case HELL -> 6;
        };

        return baseCap + difficultyBonus;
    }

    // Calculate level based on base level, floor, config, elite status, and player
    // level
    private static int calculateLevel(int baseLevel, int floorNumber, MonsterConfig config,
            boolean elite, int playerLevel) {
        int level = baseLevel + floorNumber + config.levelBonus;

        if (elite) {
            // Elite level scaling based on difficulty
            int eliteBonus = switch (config.difficulty) {
                case TUTORIAL, EASY, NORMAL -> 1 + (int) (Math.random() * 5); // +1 to +5
                case HARD, NIGHTMARE -> 5 + (int) (Math.random() * 6); // +5 to +10
                case HELL -> 8 + (int) (Math.random() * 8); // +8 to +15
            };
            level += eliteBonus;
        }

        int maxLevel = calculateMaxLevel(playerLevel, floorNumber, config.difficulty);
        return Math.min(level, maxLevel); // Cap level to prevent over-scaling, issue with previous iteration.
    }

    // Determine final rarity on template, elite, difficulty
    private static Rarity calculateRarity(MonsterTemplate template, boolean elite, Difficulty difficulty) {
        if (!elite) {
            return template.getBaseRarity();
        }

        // Elite rarity scaling by difficulty
        return switch (difficulty) {
            case TUTORIAL -> Rarity.COMMON; // No elite boost in tutorial - playing it safe.
            case EASY -> Rarity.UNCOMMON;
            case NORMAL, HARD -> Rarity.RARE;
            case NIGHTMARE, HELL -> Rarity.LEGENDARY;
        };
    }

    /**
     * Creates a random encounter monster appropriate for the current
     * floor/difficulty.
     * 
     * @param level      Base monster level (typically floor number give or take
     *                   some)
     * @param elite      Whether this is an elite variant (buffed / stronger stats)
     * @param difficulty Current game difficulty (for scaling)
     * @param rng        Random number generator (for template & pool selection)
     * @return Fully configured monster (name, level, rarity, element, etc.)
     */
    public static Monster createEncounterMonster(int level, boolean elite, Difficulty difficulty, RNG rng) {
        // Select template based on difficulty and floor progression
        MonsterTemplate[] pool = COMMON_TEMPLATES;
        int roll = rng.nextInt(100);

        if (level >= 10 && difficulty.ordinal() >= Difficulty.NORMAL.ordinal()) {
            if (roll < 25) {
                pool = RARE_TEMPLATES;
            } else {
                pool = UNCOMMON_TEMPLATES;
            }
        } else if (level > 5 && difficulty.ordinal() >= Difficulty.NORMAL.ordinal()) {
            pool = UNCOMMON_TEMPLATES;
        }

        if (pool.length == 0) {
            throw new IllegalStateException("No monster templates available for encounters");
        }

        MonsterTemplate template = pool[rng.nextInt(pool.length)];
        MonsterConfig config = MonsterConfig.fromDifficulty(difficulty);

        return createFromTemplate(template, level, level, elite, config);
    }

    // Legacy method for simpler standard monster creation (for testing / working with old saves.)
    public static Monster createStandardMonster(String name, int level, boolean elite) {
        Rarity rarity = elite ? Rarity.RARE : Rarity.COMMON;
        String finalName = elite ? "Elite " + name : name;
        return new Monster(finalName, ElementType.DARK, ElementType.LIGHT, rarity, level);
    }

    // Treasure Guardian !
    public static Monster createTreasureGuardian(int level) {
        MonsterConfig config = MonsterConfig.fromDifficulty(Difficulty.NORMAL);
        return createFromTemplate(TREASURE_GUARDIAN, level, level, false, config);
    }

    // Mimic
    public static Monster createMimic(int level) {
        MonsterConfig config = MonsterConfig.fromDifficulty(Difficulty.NORMAL);
        return createFromTemplate(MIMIC, level, level, false, config);
    }

    // Puzzle Guardian (more or less a mini-boss)
    public static Monster createPuzzleGuardian(int level) {
        MonsterConfig config = MonsterConfig.fromDifficulty(Difficulty.NORMAL);
        return createFromTemplate(PUZZLE_GUARDIAN, level + 2, level, false, config); // +2 for extra challenge
    }

    /**
     * Creates a boss monster for the current floor.
     * Bosses are significantly stronger and use legendary/mythical rarities
     * 
     * @param playerLevel Current player level for scaling
     * @param floorNumber Current floor number
     * @param difficulty  Game difficulty
     * @param rng         Random number generator (pool) - the overuse of RNG is not
     *                    ideal, but it's simpler for me.
     * @return Boss monster with enhanced stats
     */

    public static Monster createBoss(int playerLevel, int floorNumber, Difficulty difficulty, RNG rng) {
        if (BOSS_TEMPLATES.length == 0) {
            throw new IllegalStateException("No boss templates configured");
        }

        MonsterTemplate template = BOSS_TEMPLATES[rng.nextInt(BOSS_TEMPLATES.length)];
        MonsterConfig config = MonsterConfig.forBoss(difficulty);

        // Boss rarity based on difficulty
        Rarity bossRarity = switch (difficulty) {
            case TUTORIAL -> Rarity.UNCOMMON;
            case EASY, NORMAL -> Rarity.RARE;
            case HARD -> Rarity.LEGENDARY;
            case NIGHTMARE, HELL -> Rarity.MYTHICAL;
        };

        Monster boss = createFromTemplate(template, playerLevel, floorNumber, false, config, bossRarity);
        boss.applyDifficulty(config.statMultiplier); // Apply boss stat boost - making bosses more fun (?)

        return boss;
    }

    // Custom Monster creator
    public static Monster createCustomMonster(String name, ElementType element, ElementType weakness,
            Rarity rarity, int level) {
        return new Monster(name, element, weakness, rarity, level);
    }

    // Creation from template with rarity calculation
    private static Monster createFromTemplate(MonsterTemplate template, int baseLevel, int floorNumber,
            boolean elite, MonsterConfig config) {
        Rarity rarity = calculateRarity(template, elite, config.difficulty);
        return createFromTemplate(template, baseLevel, floorNumber, elite, config, rarity);
    }

    // Creation from template with specified rarity
    private static Monster createFromTemplate(MonsterTemplate template, int baseLevel, int floorNumber,
            boolean elite, MonsterConfig config, Rarity rarity) {
        int level = calculateLevel(baseLevel, floorNumber, config, elite, baseLevel);
        String name = buildMonsterName(template, elite, rarity);

        Monster monster = new Monster(name, template.getElement(), template.getWeakness(), rarity, level);
        monster.setSpeedMultiplier(template.getSpeedMultiplier());
        monster.setStrengthMultiplier(template.getStrengthMultiplier());
        return monster;
    }

    // Display name builder
    private static String buildMonsterName(MonsterTemplate template, boolean elite, Rarity rarity) {
        StringBuilder name = new StringBuilder();

        if (elite) {
            name.append("Elite ");
        }

        name.append(template.getBaseName());

        // Add boss suffix (not ideal, but works for now)
        if (rarity == Rarity.LEGENDARY || rarity == Rarity.MYTHICAL) {
            if (!template.getBaseName().contains("Boss")) {
                name.append(" (Boss)");
            }
        }

        return name.toString();
    }

    // Return all templayes
    public static DynArray<MonsterTemplate> getAllTemplates() {
        DynArray<MonsterTemplate> templates = new DynArray<>();

        // Add all templates
        for (MonsterTemplate t : COMMON_TEMPLATES)
            templates.add(t);
        for (MonsterTemplate t : UNCOMMON_TEMPLATES)
            templates.add(t);
        for (MonsterTemplate t : RARE_TEMPLATES)
            templates.add(t);
        for (MonsterTemplate t : BOSS_TEMPLATES)
            templates.add(t);
        templates.add(MIMIC);
        templates.add(TREASURE_GUARDIAN);
        templates.add(PUZZLE_GUARDIAN);

        return templates;

        // This isn't really dynamic, but it works for now.
        // If I had more time, I'd have gone for a far more data-driven approach with
        // JSON,
        // or even gone for a special class to create custom monsters and etc.
        // but alas, time is limited.
    }

    // Get template by name
    public static MonsterTemplate getTemplateByName(String name) {
        DynArray<MonsterTemplate> all = getAllTemplates();
        for (int i = 0; i < all.size(); i++) {
            MonsterTemplate t = all.get(i);
            if (t.getBaseName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null; // Template not found - should not happen if used correctly
    }
}
