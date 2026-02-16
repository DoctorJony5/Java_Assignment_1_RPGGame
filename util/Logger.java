package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger - Long Output Generic Generator Engine for Reports (a great acronym)
 * Debug and info logging system
 * 
 * PURPOSE: Provide comprehensive logging for debugging and tracking game state
 * - Console output for immediate feedback
 * - File output for persistent logs
 * - 4 log types (DEBUG, INFO, WARN, ERROR)
 * - Timestamp and category tracking
 */
public class Logger {

    private static Logger instance;

    private boolean debugEnabled;
    private boolean fileLoggingEnabled;
    private PrintWriter logFile;
    private DateTimeFormatter timeFormat;

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private Logger() {
        this.debugEnabled = false;
        this.fileLoggingEnabled = false;
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Initialize logger with options
     * 
     * @param debug Enable debug logging
     */
    public void initialize(boolean debug, boolean fileLogging) {
        this.debugEnabled = debug;
        this.fileLoggingEnabled = fileLogging;

        if (fileLogging) {
            try {
                String filename = "game_log_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log";
                logFile = new PrintWriter(new FileWriter(filename, true));
                info("LOGGER", "Log file created: " + filename);
            } catch (IOException e) {
                System.err.println("[ERROR] Could not create log file: " + e.getMessage());
                fileLoggingEnabled = false;
            }
        }
    }

    /**
     * Log a debug message (only if debug enabled)
     */
    public void debug(String category, String message) {
        if (debugEnabled) {
            log(LogLevel.DEBUG, category, message);
        }
    }

    /**
     * Log an info message
     */
    public void info(String category, String message) {
        log(LogLevel.INFO, category, message);
    }

    /**
     * Log a warning message
     */
    public void warn(String category, String message) {
        log(LogLevel.WARN, category, message);
    }

    /**
     * Log an error message
     */
    public void error(String category, String message) {
        log(LogLevel.ERROR, category, message);
    }

    /**
     * Log an error with exception
     */
    public void error(String category, String message, Exception e) {
        log(LogLevel.ERROR, category, message + " - " + e.getMessage());
        if (debugEnabled && fileLoggingEnabled && logFile != null) {
            e.printStackTrace(logFile);
        }
    }

    /**
     * Internal log method with null parameter validation
     */
    private void log(LogLevel level, String category, String message) {
        // Validate and sanitize null parameters
        if (category == null) {
            category = "UNKNOWN";
        }
        if (message == null) {
            message = "[null message passed to logger]";
        }

        String timestamp = LocalDateTime.now().format(timeFormat);
        String logMessage = String.format("[%s] [%s] [%s] %s",
                timestamp, level, category, message);

        // Console output
        if (level == LogLevel.ERROR) {
            System.err.println(logMessage);
        } else if (debugEnabled || level == LogLevel.WARN || level == LogLevel.ERROR) {
            System.out.println(logMessage);
        }

        // File output - only flush on important messages for performance
        if (fileLoggingEnabled && logFile != null) {
            logFile.println(logMessage);
            // Only flush for important messages to reduce I/O overhead
            // ERROR and WARN messages are flushed immediately to ensure they're saved
            // DEBUG and INFO accumulate in buffer for better performance
            if (level == LogLevel.ERROR || level == LogLevel.WARN) {
                logFile.flush();
            }
        }
    }

    /**
     * Close the logger and file
     */
    public void close() {
        if (logFile != null) {
            info("LOGGER", "Closing log file");
            logFile.close();
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}
