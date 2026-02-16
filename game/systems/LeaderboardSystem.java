// Jonathan Decondé - 3196362

package game.systems;

import util.DynArray;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Leaderboard!
// There's a score in the game, which the player can know. This score is also saved.
public class LeaderboardSystem {
    private static final String LEADERBOARD_FILE = "saves/leaderboard.txt";
    private static final int MAX_ENTRIES = 10;

    public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
        public String playerName;
        public long score;
        public int level;
        public int experience;
        public int monstersDefeated;
        public long runSeed;
        public String timestamp;

        public LeaderboardEntry(String playerName, long score, int level, int experience, int monstersDefeated,
                long runSeed) {
            this.playerName = playerName;
            this.score = score;
            this.level = level;
            this.experience = experience;
            this.monstersDefeated = monstersDefeated;
            this.runSeed = runSeed;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } // Quite self evident / explanatory, but we store all the above information for the player score

        @Override
        public int compareTo(LeaderboardEntry other) {
            // Sort by score descending
            return Long.compare(other.score, this.score);
        }

        @Override
        public String toString() {
            return String.format("%s | Score: %d | Lv %d | Exp: %d | Kills: %d | Seed: %d | %s",
                    playerName, score, level, experience, monstersDefeated, runSeed, timestamp);
        } // Lovely formatting / kinda clean.
    }

    // Adding entries to leader board
    public static void addEntry(String playerName, long score, int level, int experience, int monstersDefeated,
            long runSeed) {
        try {
            File file = new File(LEADERBOARD_FILE);
            file.getParentFile().mkdirs();

            DynArray<LeaderboardEntry> entries = loadLeaderboard();
            entries.add(new LeaderboardEntry(playerName, score, level, experience, monstersDefeated, runSeed));

            // Sort by score
            sortEntries(entries);

            // Keep only top 10
            // not really needed
            DynArray<LeaderboardEntry> topEntries = new DynArray<>(MAX_ENTRIES);
            for (int i = 0; i < Math.min(MAX_ENTRIES, entries.size()); i++) {
                topEntries.add(entries.get(i));
            }

            // Write back to file
            try (FileWriter fw = new FileWriter(file)) {
                for (int i = 0; i < topEntries.size(); i++) {
                    fw.write(topEntries.get(i).toString() + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving to leaderboard: " + e.getMessage());
        }
    }

    // Load from the file
    public static DynArray<LeaderboardEntry> loadLeaderboard() {
        DynArray<LeaderboardEntry> entries = new DynArray<>(); // this really doesn't need to be a dynamic array, because it's limited to size 10... But this makes things a little easier because I did change the value a few times.
        File file = new File(LEADERBOARD_FILE);

        if (!file.exists()) {
            return entries;
        } // if file does not exist - it always should though.

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                LeaderboardEntry entry = parseLeaderboardEntry(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading leaderboard: " + e.getMessage());
        } // This is a reader, where it takes a line and then passes it to a parser. If there's nothing, it does nothing.
        // This may not work if there's, for example, a space.

        return entries;
    }

    // Parsing!
    // Parsing is quite fun, and because we know how we SAVE data, we know how to open it.

    private static LeaderboardEntry parseLeaderboardEntry(String line) {
        try {
            // Format: PlayerName | Score: X | Lv Y | Exp: Z | Kills: K | timestamp
            String[] parts = line.split(" \\| "); // So here we just remove the |
            if (parts.length < 5)
                return null; // There'll ALWAYS be 5 things. So if there's less, there's an error.

            String playerName = parts[0]; // Name is simple
            long score = Long.parseLong(parts[1].replace("Score: ", "")); // Remove the Score: part
            int level = Integer.parseInt(parts[2].replace("Lv ", "")); // Remove the level part
            int experience = Integer.parseInt(parts[3].replace("Exp: ", "")); // Remove the exp part
            int kills = Integer.parseInt(parts[4].replace("Kills: ", "")); // Remove the kills part
            long seed = 0L;

            if (parts.length > 5 && parts[5].startsWith("Seed")) {
                String seedPart = parts[5].replace("Seed: ", "");
                try {
                    seed = Long.parseLong(seedPart);
                } catch (NumberFormatException ignored) {
                    seed = 0L;
                }
            }

            LeaderboardEntry entry = new LeaderboardEntry(playerName, score, level, experience, kills, seed);
            if (parts.length > 5) {
                entry.timestamp = parts[parts.length - 1];
            }
            return entry; // Simple add to
        } catch (Exception e) {
            return null;
        } // Shouldn't happen, but just in case!
    }

    // Buble sort to sort / arrange in descending order (best to worst)
    private static void sortEntries(DynArray<LeaderboardEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            for (int j = 0; j < entries.size() - 1 - i; j++) {
                if (entries.get(j).compareTo(entries.get(j + 1)) > 0) {
                    // Swap by removing and re-adding
                    LeaderboardEntry temp = entries.remove(j);
                    entries.add(j + 1, temp);
                    j++; // Skip the swapped element
                }
            }
        }
    }

    // Is score in top 10 ?
    public static boolean isTopScore(long score) {
        DynArray<LeaderboardEntry> entries = loadLeaderboard();
        if (entries.size() < MAX_ENTRIES) {
            return true; // Always in top 10 if less than 10 entries
        }
        return score > entries.get(MAX_ENTRIES - 1).score;
    }
}
