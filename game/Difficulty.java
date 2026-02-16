// Jonathan Decondé - 3196362
package game;

// Difficulty
// These are more or less the same from since the beginning of this project.
public enum Difficulty {
    TUTORIAL(0, 0.4, "Tutorial", "Very Easy", 5),
    EASY(1, 0.65, "Easy", "Beginner friendly", 10),
    NORMAL(2, 0.85, "Normal", "Standard difficulty", 20),
    HARD(3, 1.2, "Hard", "Challenging", 30),
    NIGHTMARE(4, 1.6, "Nightmare", "Extreme difficulty", 40),
    HELL(5, 2.3, "Hell", "For the brave", 50);

    public final int level;
    public final double enemyStatMultiplier;
    public final String displayName;
    public final String description;
    public final int firstExitFloor; // Floor where exit first appears

    Difficulty(int level, double enemyStatMultiplier, String displayName, String description, int firstExitFloor) {
        this.level = level;
        this.enemyStatMultiplier = enemyStatMultiplier;
        this.displayName = displayName;
        this.description = description;
        this.firstExitFloor = firstExitFloor;
    }
}
