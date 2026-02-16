// Jonathan Decondé - 3196362
package game.systems.puzzles;

import game.ui.View;
import util.Logger;
import util.RNG;

public class SphinxPuzzle {
    private View view;
    private RNG rng;
    private Logger logger;
    
    private static class Riddle {
        String question;
        String answer;
        String[] hints;
        
        Riddle(String question, String answer, String[] hints) {
            this.question = question;
            this.answer = answer;
            this.answer = answer.toLowerCase().trim();
            this.hints = hints;
        }
    }
    
    private Riddle[] riddles;
    
    public SphinxPuzzle(View view, RNG rng) {
        this.view = view;
        this.rng = rng;
        this.logger = Logger.getInstance();
        initRiddles();
    }
    
    private void initRiddles() {
        riddles = new Riddle[] {
            new Riddle(
                "I speak without a mouth and hear without ears. I have no body, but come alive with wind. What am I?",
                "echo",
                new String[] {
                    "I can be heard in caves and canyons.",
                    "I repeat what you say.",
                    "Sound bounces to create me."
                }
            ),
            new Riddle(
                "The more you take, the more you leave behind. What am I?",
                "footsteps",
                new String[] {
                    "Think about walking.",
                    "You create a trail.",
                    "They mark your journey."
                }
            ),
            new Riddle(
                "What has keys but no locks, space but no room, and you can enter but can't go inside?",
                "keyboard",
                new String[] {
                    "It's used for writing.",
                    "Found with computers.",
                    "You press its keys to type."
                }
            ),
            new Riddle(
                "I am always hungry and must always be fed. The finger I touch will soon turn red. What am I?",
                "fire",
                new String[] {
                    "I need fuel to survive.",
                    "I can burn you.",
                    "I produce heat and light."
                }
            ),
            new Riddle(
                "What can travel around the world while staying in a corner?",
                "stamp",
                new String[] {
                    "Found on envelopes.",
                    "Used for mail.",
                    "It sits in the corner of letters."
                }
            ),
            new Riddle(
                "I have cities but no houses, forests but no trees, and water but no fish. What am I?",
                "map",
                new String[] {
                    "I show locations.",
                    "Used for navigation.",
                    "I represent places on paper."
                }
            ),
            new Riddle(
                "What gets wetter the more it dries?",
                "towel",
                new String[] {
                    "Found in bathrooms.",
                    "Used after washing.",
                    "It absorbs water."
                }
            ),
            new Riddle(
                "I have branches, but no fruit, trunk, or leaves. What am I?",
                "bank",
                new String[] {
                    "It deals with money.",
                    "You store valuables there.",
                    "It has multiple locations."
                }
            ),
            new Riddle(
                "What begins with T, ends with T, and has T in it?",
                "teapot",
                new String[] {
                    "It holds a beverage.",
                    "You pour from it.",
                    "Used for making tea."
                }
            ),
            new Riddle(
                "I am not alive, but I grow. I don't have lungs, but I need air. I don't have a mouth, but water kills me. What am I?",
                "fire",
                new String[] {
                    "I spread and consume.",
                    "I need oxygen.",
                    "I can be extinguished with water."
                }
            )
        };
        logger.debug("PUZZLE", "Sphinx riddles initialized: " + riddles.length + " riddles");
    }
    
    public boolean play() {
        logger.info("PUZZLE", "Starting Sphinx puzzle");
        
        view.showMessage("═══════════════════════════════════════════════════════════");
        view.showMessage("  THE SPHINX'S RIDDLE");
        view.showMessage("═══════════════════════════════════════════════════════════");
        view.showMessage("");
        view.showMessage("A majestic Sphinx blocks your path!");
        view.showMessage("'Answer my riddle correctly, and you may pass.'");
        view.showMessage("'Fail, and face my wrath...'");
        view.showMessage("");
        
        // Select random riddle
        Riddle riddle = riddles[rng.nextInt(riddles.length)];
        logger.debug("PUZZLE", "Selected riddle: " + riddle.question);
        
        view.showMessage("The Sphinx asks:");
        view.showMessage("");
        view.showMessage("  \"" + riddle.question + "\"");
        view.showMessage("");
        
        int attempts = 3;
        int hintsUsed = 0;
        
        while (attempts > 0) {
            view.showMessage("You have " + attempts + " attempt(s) remaining.");
            view.showMessage("");
            view.showMessage("Enter your answer (or 'HINT' for a clue, or 'GIVE UP' to flee):");
            String input = view.readLine().trim().toLowerCase();
            
            logger.debug("PUZZLE", "Player input: " + input);
            
            if (input.equals("give up")) {
                view.showMessage("");
                view.showMessage("You flee from the Sphinx's challenge!");
                view.showMessage("The Sphinx laughs at your cowardice...");
                logger.info("PUZZLE", "Player gave up on Sphinx riddle");
                return false;
            }
            
            if (input.equals("hint")) {
                if (hintsUsed < riddle.hints.length) {
                    view.showMessage("");
                    view.showMessage("The Sphinx offers a hint:");
                    view.showMessage("  \"" + riddle.hints[hintsUsed] + "\"");
                    view.showMessage("");
                    logger.debug("PUZZLE", "Player used hint " + (hintsUsed + 1));
                    hintsUsed++;
                } else {
                    view.showMessage("");
                    view.showMessage("The Sphinx has no more hints to give!");
                    view.showMessage("");
                }
                continue;
            }
            
            // Check answer
            if (input.equals(riddle.answer)) {
                view.showMessage("");
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("  CORRECT!");
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("");
                view.showMessage("The Sphinx nods approvingly.");
                view.showMessage("'You have wisdom beyond your years, mortal.'");
                view.showMessage("'You may pass.'");
                view.showMessage("");
                logger.info("PUZZLE", "Player solved Sphinx riddle in " + (4 - attempts) + " attempt(s)");
                return true;
            } else {
                attempts--;
                if (attempts > 0) {
                    view.showMessage("");
                    view.showMessage("The Sphinx shakes its head.");
                    view.showMessage("'Incorrect. Think harder...'");
                    view.showMessage("");
                } else {
                    view.showMessage("");
                    view.showMessage("═══════════════════════════════════════════════════════════");
                    view.showMessage("  FAILURE!");
                    view.showMessage("═══════════════════════════════════════════════════════════");
                    view.showMessage("");
                    view.showMessage("The Sphinx roars in anger!");
                    view.showMessage("'You have failed! The answer was: " + riddle.answer + "'");
                    view.showMessage("'Now face the consequences!'");
                    view.showMessage("");
                    logger.info("PUZZLE", "Player failed Sphinx riddle");
                    return false;
                }
            }
        }
        
        return false;
    }
}
