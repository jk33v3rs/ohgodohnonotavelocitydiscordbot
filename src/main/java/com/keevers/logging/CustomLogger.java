package com.keevers.logging;

import java.nio.file.Path;

public class CustomLogger {
    private static final CustomLogger instance = new CustomLogger();

    private CustomLogger() {}

    public static CustomLogger getLogger() {
        return instance;
    }

    public static void setup(Path dataDirectory) {
        // Implement setup logic if needed
        System.out.println("Logger setup with data directory: " + dataDirectory);
    }

    public void info(String message) {
        System.out.println("INFO: " + message);
    }

    public void info(String format, Object... args) {
        System.out.println("INFO: " + String.format(format, args));
    }

    public void severe(String message) {
        System.err.println("SEVERE: " + message);
    }

    public void error(String message, Exception e) {
        System.err.println("ERROR: " + message);
        e.printStackTrace();
    }

    public void error(String message) {
        System.err.println("ERROR: " + message);
    }

    public void error(String format, Object... args) {
        System.err.println("ERROR: " + String.format(format, args));
    }

    // New method that takes three arguments (String, String, Exception)
    public void error(String message1, String message2, Exception e) {
        System.err.println("ERROR: " + message1 + " " + message2);
        e.printStackTrace();
    }
}