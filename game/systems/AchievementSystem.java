// Jonathan Decondé - 3196362
package game.systems;

import util.DynArray;
import util.Logger;

// This is a basic achievement system that tracks and unlocks achievements based on player actions
// This isn't implemented - probably won't happen
// Would have been funny. Put more effort into this than some other important systems :/

public class AchievementSystem {
    private static class Achievement {
        String id;
        String name;
        String description;
        boolean unlocked;

        Achievement(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.unlocked = false;
        }
    }

    private DynArray<Achievement> achievements;
    private Logger logger;

    public AchievementSystem() {
        this.logger = Logger.getInstance();
        this.achievements = new DynArray<>();
        initializeAchievements();
    }

    private void initializeAchievements() {

        achievements.add(new Achievement("first_blood", "First Blood", "Defeat your first monster"));
        achievements.add(new Achievement("monster_slayer_10", "Monster Slayer", "Defeat 10 monsters"));
        achievements.add(new Achievement("monster_slayer_50", "Monster Hunter", "Defeat 50 monsters"));
        achievements.add(new Achievement("monster_slayer_100", "Exterminator", "Defeat 100 monsters"));
        achievements.add(new Achievement("boss_killer", "Dragon Slayer", "Defeat 5 boss monsters"));
        achievements.add(new Achievement("elite_hunter", "Elite Hunter", "Defeat 10 elite enemies"));

        achievements.add(new Achievement("floor_5", "Depth Diver", "Reach floor 5"));
        achievements.add(new Achievement("floor_10", "Deep Explorer", "Reach floor 10"));
        achievements.add(new Achievement("floor_20", "Master of the Maze", "Reach floor 20"));
        achievements.add(new Achievement("floor_50", "Legend of the Labyrinth", "Reach floor 50"));
        achievements.add(new Achievement("explorer", "Cartographer", "Explore 100 rooms"));

        achievements.add(new Achievement("level_5", "Seasoned Adventurer", "Reach level 5"));
        achievements.add(new Achievement("level_10", "Veteran Explorer", "Reach level 10"));
        achievements.add(new Achievement("level_15", "Hero of Legend", "Reach level 15"));
        achievements.add(new Achievement("level_20", "Demigod", "Reach level 20"));

        achievements.add(new Achievement("puzzle_master", "Puzzle Master", "Solve 5 puzzles"));
        achievements.add(new Achievement("puzzle_genius", "Enigma Solver", "Solve 20 puzzles"));
        achievements.add(new Achievement("lore_collector", "Lore Seeker", "Find 5 lore items"));
        achievements.add(new Achievement("lore_complete", "Chronicler", "Find all 10 lore items"));

        achievements.add(new Achievement("mimic_hunter", "Mimic Hunter", "Defeat 3 mimic chests"));
        achievements.add(new Achievement("mimic_nightmare", "Mimic's Bane", "Defeat 10 mimic chests"));
        achievements.add(new Achievement("treasure_hunter", "Treasure Hunter", "Open 20 treasure chests"));
        achievements.add(new Achievement("treasure_hoarder", "Hoarder", "Open 100 treasure chests"));
        achievements.add(new Achievement("rare_collector", "Rare Collector", "Obtain 5 rare items"));
        achievements.add(new Achievement("rare_collector_2", "Curator of the British Museum", "Obtain 100 rare items"));
        achievements.add(new Achievement("legendary_collector", "Legendary Arsenal", "Obtain a legendary item"));

        logger.debug("ACHIEVEMENT", "Initialized " + achievements.size() + " achievements");
    }

    public boolean unlock(String achievementId) {
        for (int i = 0; i < achievements.size(); i++) {
            Achievement ach = achievements.get(i);
            if (ach.id.equals(achievementId) && !ach.unlocked) {
                ach.unlocked = true;
                logger.info("ACHIEVEMENT", "Unlocked: " + ach.name);
                return true; // Return true if newly unlocked
            }
        }
        return false;
    }

    public Achievement getAchievement(String achievementId) {
        for (int i = 0; i < achievements.size(); i++) {
            if (achievements.get(i).id.equals(achievementId)) {
                return achievements.get(i);
            }
        }
        return null;
    }

    public DynArray<Achievement> getAllAchievements() {
        return achievements;
    }

    public int getUnlockedCount() {
        int count = 0;
        for (int i = 0; i < achievements.size(); i++) {
            if (achievements.get(i).unlocked) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return achievements.size();
    }

    // TODO: Implement this to actually work / happen
    // Maybe make a little more interesting ? What even is the point of these again.
}
