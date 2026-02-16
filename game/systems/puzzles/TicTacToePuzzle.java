// Jonathan Decondé - 3196362
package game.systems.puzzles;

import game.ui.View;
import util.Logger;
import util.RNG;

public class TicTacToePuzzle {
    private char[][] board;
    private View view;
    private RNG rng;
    private Logger logger;
    
    private static final char EMPTY = ' ';
    private static final char PLAYER = 'X';
    private static final char AI = 'O';
    
    public TicTacToePuzzle(View view, RNG rng) {
        this.view = view;
        this.rng = rng;
        this.logger = Logger.getInstance();
        this.board = new char[3][3];
        initBoard();
    }
    
    private void initBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = EMPTY;
            }
        }
        logger.debug("PUZZLE", "Tic Tac Toe board initialized");
    }
    
    public boolean play() {
        logger.info("PUZZLE", "Starting Tic Tac Toe puzzle");
        view.showMessage("═══════════════════════════════════════════════════════════");
        view.showMessage("  TIC TAC TOE PUZZLE");
        view.showMessage("═══════════════════════════════════════════════════════════");
        view.showMessage("A mysterious force challenges you to Tic Tac Toe!");
        view.showMessage("Win to proceed, lose and face the consequences...");
        view.showMessage("You are X, the dungeon spirit is O.");
        view.showMessage("");
        
        boolean playerTurn = true;
        
        while (true) {
            displayBoard();
            
            if (playerTurn) {
                if (!makePlayerMove()) {
                    logger.warn("PUZZLE", "Player forfeited Tic Tac Toe");
                    return false;
                }
            } else {
                makeAIMove();
            }
            
            // Check for win
            char winner = checkWinner();
            if (winner == PLAYER) {
                displayBoard();
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("  VICTORY!");
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("You have outwitted the dungeon spirit!");
                logger.info("PUZZLE", "Player won Tic Tac Toe");
                return true;
            } else if (winner == AI) {
                displayBoard();
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("  DEFEAT!");
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("The dungeon spirit has beaten you!");
                logger.info("PUZZLE", "Player lost Tic Tac Toe");
                return false;
            } else if (isBoardFull()) {
                displayBoard();
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("  DRAW!");
                view.showMessage("═══════════════════════════════════════════════════════════");
                view.showMessage("Neither side could claim victory. You may pass.");
                logger.info("PUZZLE", "Tic Tac Toe ended in draw");
                return true; // Draw counts as success
            }
            
            playerTurn = !playerTurn;
        }
    }
    
    private void displayBoard() {
        view.showMessage("");
        view.showMessage("     1   2   3");
        view.showMessage("   ┌───┬───┬───┐");
        for (int i = 0; i < 3; i++) {
            view.showMessage(" " + (i + 1) + " │ " + board[i][0] + " │ " + board[i][1] + " │ " + board[i][2] + " │");
            if (i < 2) {
                view.showMessage("   ├───┼───┼───┤");
            }
        }
        view.showMessage("   └───┴───┴───┘");
        view.showMessage("");
    }
    
    private boolean makePlayerMove() {
        view.showMessage("Enter your move (row col), e.g., '1 2' or 'Q' to forfeit:");
        String input = view.readLine().trim().toUpperCase();
        
        logger.debug("PUZZLE", "Player input: " + input);
        
        if (input.equals("Q")) {
            return false;
        }
        
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 2) {
                view.showMessage("Invalid input! Please enter row and column (1-3).");
                return makePlayerMove();
            }
            
            int row = Integer.parseInt(parts[0]) - 1;
            int col = Integer.parseInt(parts[1]) - 1;
            
            if (row < 0 || row > 2 || col < 0 || col > 2) {
                view.showMessage("Invalid position! Row and column must be between 1 and 3.");
                return makePlayerMove();
            }
            
            if (board[row][col] != EMPTY) {
                view.showMessage("That position is already taken!");
                return makePlayerMove();
            }
            
            board[row][col] = PLAYER;
            logger.debug("PUZZLE", "Player placed X at [" + row + "][" + col + "]");
            return true;
            
        } catch (NumberFormatException e) {
            view.showMessage("Invalid input! Please enter numbers.");
            return makePlayerMove();
        }
    }
    
    private void makeAIMove() {
        // Try to win
        int[] move = findWinningMove(AI);
        if (move != null) {
            board[move[0]][move[1]] = AI;
            logger.debug("PUZZLE", "AI made winning move at [" + move[0] + "][" + move[1] + "]");
            return;
        }
        
        // Block player from winning
        move = findWinningMove(PLAYER);
        if (move != null) {
            board[move[0]][move[1]] = AI;
            logger.debug("PUZZLE", "AI blocked player at [" + move[0] + "][" + move[1] + "]");
            return;
        }
        
        // Take center if available
        if (board[1][1] == EMPTY) {
            board[1][1] = AI;
            logger.debug("PUZZLE", "AI took center");
            return;
        }
        
        // Take a random corner
        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        for (int i = 0; i < corners.length; i++) {
            int idx = rng.nextInt(corners.length);
            int row = corners[idx][0];
            int col = corners[idx][1];
            if (board[row][col] == EMPTY) {
                board[row][col] = AI;
                logger.debug("PUZZLE", "AI took corner at [" + row + "][" + col + "]");
                return;
            }
        }
        
        // Take any available space
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = AI;
                    logger.debug("PUZZLE", "AI took random spot at [" + i + "][" + j + "]");
                    return;
                }
            }
        }
    }
    
    private int[] findWinningMove(char player) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == EMPTY) return new int[]{i, 2};
            if (board[i][0] == player && board[i][2] == player && board[i][1] == EMPTY) return new int[]{i, 1};
            if (board[i][1] == player && board[i][2] == player && board[i][0] == EMPTY) return new int[]{i, 0};
        }
        
        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == EMPTY) return new int[]{2, j};
            if (board[0][j] == player && board[2][j] == player && board[1][j] == EMPTY) return new int[]{1, j};
            if (board[1][j] == player && board[2][j] == player && board[0][j] == EMPTY) return new int[]{0, j};
        }
        
        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == EMPTY) return new int[]{2, 2};
        if (board[0][0] == player && board[2][2] == player && board[1][1] == EMPTY) return new int[]{1, 1};
        if (board[1][1] == player && board[2][2] == player && board[0][0] == EMPTY) return new int[]{0, 0};
        
        if (board[0][2] == player && board[1][1] == player && board[2][0] == EMPTY) return new int[]{2, 0};
        if (board[0][2] == player && board[2][0] == player && board[1][1] == EMPTY) return new int[]{1, 1};
        if (board[1][1] == player && board[2][0] == player && board[0][2] == EMPTY) return new int[]{0, 2};
        
        return null;
    }
    
    private char checkWinner() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return board[i][0];
            }
        }
        
        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] != EMPTY && board[0][j] == board[1][j] && board[1][j] == board[2][j]) {
                return board[0][j];
            }
        }
        
        // Check diagonals
        if (board[0][0] != EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return board[0][0];
        }
        if (board[0][2] != EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return board[0][2];
        }
        
        return EMPTY;
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
}
