// Jonathan Decondé - 3196362

package game.systems;

import game.entities.Player;
import game.systems.dungeons.DungeonFloor;
import util.ConsoleColor;
import util.Logger;

// The Dungeon Master system was a idea for better game development
// The concept had been to have a dungeon master, like in DnD, who responds to player and tries to make the dungeon more interesting to them
// For example, ideally, a more "slayer" player (i.e. going more after monsters and stuff) would have led to more monsters being spawned, harder ones, etc.
// This however proved INCREDIBLY difficult so this is a stub.
// A lot of this code is unused, but I left it in because I might want to come back to it later

public class DungeonMasterSystem {

    public enum PlayerStyle {
        EXPLORER,
        SLAYER,
        LOOTER,
        BALANCED
    }

    public enum DMResponse {
        // DM actively adapts the dungeon to challenge the player more
        ESCALATING("ESCALATING DIFFICULTY", ConsoleColor.BRIGHT_YELLOW),
        // DM rewards the player's playstyle with favorable conditions
        REWARDING("REWARDING PLAYSTYLE", ConsoleColor.BRIGHT_GREEN),
        // DM is testing the player with mixed challenges
        TESTING("TESTING METTLE", ConsoleColor.BRIGHT_CYAN),
        // DM is giving the player a breather
        MERCIFUL("MERCIFUL REPRIEVE", ConsoleColor.BRIGHT_BLUE);

        public final String displayName;
        public final String color;

        DMResponse(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    public static class Tuning {
        public int encounterBias; // positive = more fights
        public int treasureBias; // positive = more treasure/safe rooms
        public int safeBias; // positive = more safe rooms/shops
        public PlayerStyle style = PlayerStyle.BALANCED;
        public long floorSeed;

        // NEW: Enhanced DM responsiveness
        public DMResponse dmResponse = DMResponse.TESTING;
        public int consistencyStreak = 0; // How many floors in a row with same playstyle
        public double skillMultiplier = 1.0; // Adjust difficulty based on player skill
        public boolean shouldShowDMMessage = false; // Show DM intervention to player
        public String dmMessage = ""; // What DM is doing this floor
    }

    private PlayerStyle lastStyle = PlayerStyle.BALANCED;
    private final Logger logger = Logger.getInstance();

    private int floorsSinceStyleChange = 0;
    private double playerWinRate = 0.5; // Estimate of how well player is doing (0.0 = struggling, 1.0 = dominating)
    private int consecutiveVictories = 0; // Track winning streaks - unused for now

    public long deriveFloorSeed(long runSeed, int floorNumber) {
        // Simple deterministic mix to keep seeds stable across runs/saves
        // https://alvaro-videla.com/2016/10/inside-java-s-threadlocalrandom.html
        // The system here is meant to be random, and upon reading some things about
        // randomess on computers, I thought of this
        // ThreadLocalRandom
        // This should mean that every game is unique; for better or worse.
        // This sadly doesn't work like how I wanted to.
        // The 0x..... things are quite random, though very long, numbers. The exact
        // ones I used I found on the internet.
        // You can safely ignore this, this has no real effect.
        long mixed = runSeed ^ (0x9E3779B97F4A7C15L + (long) floorNumber * 0xBF58476D1CE4E5B9L);
        mixed ^= (mixed >>> 30);
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94D049BB133111EBL;
        mixed ^= (mixed >>> 31);
        return mixed;
    }

    // Enhanced planTuning: Intelligently adapts dungeon based on playstyle,
    // performance, and consistency
    public Tuning planTuning(Player player) {
        Tuning t = new Tuning();
        t.style = lastStyle;
        t.consistencyStreak = floorsSinceStyleChange;

        // Calculate skill-based difficulty multiplier
        // If player is doing well (high level, strong gear), increase difficulty
        // If player is struggling (low health, weak gear), decrease difficulty
        double gearQuality = Math.min(player.getStrength() / 100.0, 1.0); // Strength as gear proxy
        double healthRatio = player.getHealth() / (double) player.getMaxHealth();
        t.skillMultiplier = 0.7 + (gearQuality * 0.3) + (healthRatio * 0.2);

        // Apply playstyle-specific tuning with consistency bonuses
        switch (lastStyle) {
            case EXPLORER -> {
                // Explorers get more rooms to explore and secrets to find
                t.treasureBias += 6 + (floorsSinceStyleChange / 2); // Bonus for consistent exploration
                t.safeBias += 4;
                t.encounterBias -= 4;
                t.dmResponse = floorsSinceStyleChange > 2 ? DMResponse.REWARDING : DMResponse.TESTING;

                if (floorsSinceStyleChange > 2) {
                    t.shouldShowDMMessage = true;
                    t.dmMessage = "The dungeon expands before you, revealing hidden paths...";
                }
            }
            case SLAYER -> {
                // Slayers face progressively harder encounters, especially if consistent
                t.encounterBias += 8 + (floorsSinceStyleChange * 2); // Escalates difficulty
                t.treasureBias -= 3;
                t.dmResponse = floorsSinceStyleChange > 2 ? DMResponse.ESCALATING : DMResponse.TESTING;

                if (floorsSinceStyleChange > 3) {
                    t.shouldShowDMMessage = true;
                    t.dmMessage = "The dungeon senses your bloodlust. Elite enemies emerge from the shadows...";
                }
            }
            case LOOTER -> {
                // Looters find more treasure but face occasional surprises
                t.treasureBias += 8;
                t.encounterBias -= 2;
                t.dmResponse = floorsSinceStyleChange > 2 ? DMResponse.REWARDING : DMResponse.TESTING;

                if (floorsSinceStyleChange > 2) {
                    t.shouldShowDMMessage = true;
                    t.dmMessage = "Precious items shimmer in the distance, but danger lurks nearby...";
                }
            }
            default -> {
                // BALANCED keeps defaults but still responds to skill level
                t.dmResponse = playerWinRate > 0.6 ? DMResponse.ESCALATING
                        : playerWinRate < 0.4 ? DMResponse.MERCIFUL : DMResponse.TESTING;
            }
        }

        // Apply skill-based adjustments
        if (t.skillMultiplier > 1.1) {
            // Player is strong: increase difficulty
            t.encounterBias += 3;
            t.safeBias -= 2;
        } else if (t.skillMultiplier < 0.8) {
            // Player is weak: reduce difficulty
            t.encounterBias -= 3;
            t.safeBias += 2;
        }

        return t;
    }

    public void recordFloorResult(DungeonFloor floor, Player player,
            int killsBefore, int chestsBefore, int puzzlesBefore) {
        double exploredRatio = floor.getExploredRatio();
        int killsDelta = Math.max(0, player.getMonstersDefeated() - killsBefore);
        int chestDelta = Math.max(0, player.getChestsOpened() - chestsBefore);
        int puzzleDelta = Math.max(0, player.getPuzzlesSolved() - puzzlesBefore);

        // Detect player style based on this floor's behavior
        PlayerStyle newStyle = classify(exploredRatio, killsDelta, chestDelta, puzzleDelta);

        // Track consistency: same style multiple floors in a row amplifies DM response
        if (newStyle == lastStyle) {
            floorsSinceStyleChange++;
        } else {
            floorsSinceStyleChange = 0;
            lastStyle = newStyle;
        }

        // Update performance estimate based on floor survival and completion
        // Kills are good for slayers, chests are good for looters, exploration is good
        // for explorers
        double performanceScore = 0;
        if (newStyle == PlayerStyle.SLAYER && killsDelta > 3)
            performanceScore = 0.8;
        else if (newStyle == PlayerStyle.LOOTER && chestDelta >= 3)
            performanceScore = 0.8;
        else if (newStyle == PlayerStyle.EXPLORER && exploredRatio > 0.7)
            performanceScore = 0.8;
        else if (newStyle == PlayerStyle.BALANCED && exploredRatio > 0.5 && killsDelta > 0)
            performanceScore = 0.7;
        else
            performanceScore = 0.4;

        // Track win rate using exponential moving average (recent performance weighted
        // more)
        playerWinRate = (playerWinRate * 0.7) + (performanceScore * 0.3);

        // Track victories if all objectives were met
        if (performanceScore >= 0.7) {
            consecutiveVictories++;
        } else {
            consecutiveVictories = 0;
        }

        logger.debug("DM", "Style set to " + newStyle + " (explored=" + exploredRatio + " kills=" + killsDelta
                + " chests=" + chestDelta + " puzzles=" + puzzleDelta + ") | Streak: " + floorsSinceStyleChange
                + " | Win Rate: " + String.format("%.1f%%", playerWinRate * 100));
    }

    private PlayerStyle classify(double exploredRatio, int kills, int chests, int puzzles) {
        // Simple heuristics: pick dominant behavior
        if (exploredRatio >= 0.65 && kills < 5) {
            return PlayerStyle.EXPLORER;
        }
        if (kills >= 6 && kills > chests * 2) {
            return PlayerStyle.SLAYER;
        }
        if (chests >= 4 && chests >= kills) {
            return PlayerStyle.LOOTER;
        }
        if (puzzles >= 2) {
            return PlayerStyle.EXPLORER; // puzzles imply seeking curiosities
        }
        return PlayerStyle.BALANCED;
        // These are all VERY random / arbitrary values, and in no way represent the
        // original intention / goal.
        // It's actually quite hard to make this "fun" or "work" without far more work.
        // This is very underdevelopped.
    }

    // NEW: Generate motivational or warning messages based on player performance
    public String generateDMCommentary(PlayerStyle style, int floorNumber, int streak, double winRate) {
        // Messages vary by playstyle and performance to create personalized gameplay
        // experience
        String[] slayerMessages = {
                "The echoes of battle intensify. Your bloodlust calls forth worthy opponents...",
                "Stronger creatures gather, drawn by the scent of combat. Steel yourself!",
                "Elite warriors emerge from the shadows. Your reputation precedes you...",
                "The dungeon tests your might with relentless opposition.",
                "Legends are written in blood. Make yours a story of triumph!"
        };

        String[] explorerMessages = {
                "Secret passages open before the curious. Fortune favors the bold!",
                "The dungeon reveals its mysteries to those who seek them.",
                "Ancient secrets await in forgotten corners of this realm.",
                "Your keen eyes perceive what others miss. Hidden treasures beckon!",
                "The deeper secrets of this place are yours to uncover."
        };

        String[] looterMessages = {
                "Riches glimmer in the distance. The hunt for treasure begins!",
                "Fortune smiles upon the ambitious. Valuables await discovery.",
                "Every corner holds potential wealth. The loot awaits!",
                "Greed stirs treasures from their resting places.",
                "The dungeon's riches are yours for the taking!"
        };

        String[] dangerMessages = {
                "CAUTION: You are overconfident. Danger lurks ahead!",
                "WARNING: The dungeon adapts to challenge those who dominate.",
                "BEWARE: Greater threats emerge to test your resolve.",
                "ALERT: The deepest perils stir. Prepare yourself!"
        };

        // Choose message pool based on style
        String[] pool = switch (style) {
            case SLAYER -> slayerMessages;
            case EXPLORER -> explorerMessages;
            case LOOTER -> looterMessages;
            default -> new String[] { "The dungeon continues its eternal mystery..." };
        };

        // Use streak to pick message (so consistent players get escalating flavor)
        int index = (streak % pool.length);
        String message = pool[index];

        // If player is dominating (high win rate), add warning
        if (winRate > 0.75) {
            message = dangerMessages[(floorNumber % dangerMessages.length)] + " " + message;
        }

        return message;
    }
}
