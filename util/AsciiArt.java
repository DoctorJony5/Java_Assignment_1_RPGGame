package util;

/**
 * ASCII art and display formatting utilities.
 * All methods return strings instead of printing directly (MVC pattern).
 * 
 * TODO:
 * - Add more complex art (monsters, items, etc.)
 * - Add color support (using ANSI codes) for different text types (titles, errors, etc.)
 * - Add methods for building more complex UI elements (menus, status screens, etc.)
 * 
 */
public class AsciiArt {
    private AsciiArt() {
        // Utility class
    }

    public static String buildBorder(int width, char corner, char horizontal, char vertical) {
        StringBuilder sb = new StringBuilder();
        sb.append(corner);
        for (int i = 0; i < width - 2; i++) {
            sb.append(horizontal);
        }
        sb.append(corner);
        return sb.toString();
    }

    public static String buildLine(int width, char vertical) {
        StringBuilder sb = new StringBuilder();
        sb.append(vertical);
        for (int i = 0; i < width - 2; i++) {
            sb.append(" ");
        }
        sb.append(vertical);
        return sb.toString();
    }

    public static String buildCenteredText(String text, int width, char vertical) {
        int padding = (width - 2 - text.length()) / 2;
        int rightPadding = width - 2 - padding - text.length();

        StringBuilder sb = new StringBuilder();
        sb.append(vertical);
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(text);
        for (int i = 0; i < rightPadding; i++) {
            sb.append(" ");
        }
        sb.append(vertical);
        return sb.toString();
    }

    public static String buildBox(String title, int width) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildBorder(width, '╔', '═', '║')).append("\n");
        if (title != null && !title.isEmpty()) {
            sb.append(buildCenteredText(title, width, '║')).append("\n");
            sb.append(buildBorder(width, '╠', '═', '║')).append("\n");
        }
        return sb.toString();
    }

    public static String buildBottomBorder(int width) {
        return buildBorder(width, '╚', '═', '║');
    }

    public static String buildDivider(int width) {
        return buildBorder(width, '╠', '═', '║');
    }

    public static String getHealthBar(int currentHealth, int maxHealth, int length) {
        int filledLength = (currentHealth * length) / Math.max(1, maxHealth);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    public static String getHealthBarColored(int currentHealth, int maxHealth, int length) {
        String bar = getHealthBar(currentHealth, maxHealth, length);
        int percentage = (currentHealth * 100) / Math.max(1, maxHealth);

        if (percentage > 50) {
            return ConsoleColor.colorize(bar, ConsoleColor.GREEN);
        } else if (percentage > 25) {
            return ConsoleColor.colorize(bar, ConsoleColor.YELLOW);
        } else {
            return ConsoleColor.colorize(bar, ConsoleColor.RED);
        }
    }

    public static String getManaBar(int currentMana, int maxMana, int length) {
        int filledLength = (currentMana * length) / Math.max(1, maxMana);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append("▓");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        return ConsoleColor.colorize(bar.toString(), ConsoleColor.BLUE);
    }

    public static String getTitle(String text) {
        return ConsoleColor.colorize(ConsoleColor.BOLD + text, ConsoleColor.CYAN);
    }

    public static String getError(String text) {
        return ConsoleColor.colorize(text, ConsoleColor.RED);
    }

    public static String getSuccess(String text) {
        return ConsoleColor.colorize(text, ConsoleColor.GREEN);
    }

    public static String getWarning(String text) {
        return ConsoleColor.colorize(text, ConsoleColor.YELLOW);
    }

    public static String getInfo(String text) {
        return ConsoleColor.colorize(text, ConsoleColor.BRIGHT_CYAN);
    }

    public static String getHighlight(String text) {
        return ConsoleColor.colorize(ConsoleColor.BOLD + text, ConsoleColor.BRIGHT_YELLOW);
    }

    public static String buildLogo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.CYAN)).append(
                "╔═══════════════════════════════════════════════════════════╗\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.CYAN)).append(
                "║                                                           ║\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.BRIGHT_MAGENTA)).append(
                "║         DAEDALUS'S INFINITE DUNGEON                       ║\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.BRIGHT_MAGENTA)).append(
                "║         Descend into madness and seek glory              ║\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.CYAN)).append(
                "║                                                           ║\n");
        sb.append(ConsoleColor.colorize(ConsoleColor.BOLD, ConsoleColor.CYAN)).append(
                "╚═══════════════════════════════════════════════════════════╝\n");
        sb.append("\n");
        return sb.toString();
    }

    public static String buildSmallLogo() {
        StringBuilder sb = new StringBuilder();
        sb.append(ConsoleColor.colorize("═══════════════════════════════════════════", ConsoleColor.CYAN)).append("\n");
        sb.append(ConsoleColor.colorize("  DAEDALUS'S INFINITE DUNGEON", ConsoleColor.BRIGHT_MAGENTA)).append("\n");
        sb.append(ConsoleColor.colorize("═══════════════════════════════════════════", ConsoleColor.CYAN)).append("\n");
        return sb.toString();
    }

    // ============ DEPRECATED METHODS (for backward compatibility) ============
    // These will print warnings and delegate to the new methods

    @Deprecated
    public static void printBorder(int width, char corner, char horizontal, char vertical) {
        System.out.println(buildBorder(width, corner, horizontal, vertical));
    }

    @Deprecated
    public static void printLine(int width, char vertical) {
        System.out.println(buildLine(width, vertical));
    }

    @Deprecated
    public static void printCenteredText(String text, int width, char vertical) {
        System.out.println(buildCenteredText(text, width, vertical));
    }

    @Deprecated
    public static void printBox(String title, int width) {
        System.out.print(buildBox(title, width));
    }

    @Deprecated
    public static void printBottomBorder(int width) {
        System.out.println(buildBottomBorder(width));
    }

    @Deprecated
    public static void printDivider(int width) {
        System.out.println(buildDivider(width));
    }

    @Deprecated
    public static void printLogo() {
        System.out.print(buildLogo());
    }

    @Deprecated
    public static void printSmallLogo() {
        System.out.print(buildSmallLogo());
    }
}
