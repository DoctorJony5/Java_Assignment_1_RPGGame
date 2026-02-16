// Jonathan Decondé - 3196362
package app;

import game.Game;

/**
 * Main - Launch this for stuff to happen. Magic or something.
 * There's a debug tag for debugging, though you can still do that in-game via3
 * 
 * the settings in the main menu
 * There's no real need to use debug, but it would be a hassle to remove it.
 */
public class Main {
    public static void main(String[] args) {
        boolean debugMode = false;

        // Check for debug argument
        for (String arg : args) {
            if (arg.equals("--debug") || arg.equals("-d")) {
                debugMode = true;
                System.out.println("[SYSTEM] Debug mode enabled via command line");
                System.out.println("Please restart program - not meant for users");
            }
        }

        // Start game with debug mode
        Game game = new Game(debugMode);
        game.start();
    }
}