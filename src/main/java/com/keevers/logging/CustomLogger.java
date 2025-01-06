package com.keevers.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CustomLogger {
    private static final Logger customLogger = Logger.getLogger("VelocityWhitelistLogger");

    public static Logger getLogger() {
        return customLogger;
    }

    public static void setup(Path dataDirectory) {
        try {
            Path logDir = dataDirectory;
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            FileHandler fileHandler = new FileHandler(logDir.resolve("plugin.log").toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            customLogger.addHandler(fileHandler);
            customLogger.setUseParentHandlers(false);
            customLogger.info("Custom logger initialized.");
        } catch (IOException e) {
            customLogger.severe("Failed to setup custom logger: " + e.getMessage());
        }
    }
}