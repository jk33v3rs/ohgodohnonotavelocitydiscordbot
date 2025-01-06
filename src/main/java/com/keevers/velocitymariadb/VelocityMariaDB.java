package com.keevers.velocitymariadb;

import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

public class VelocityMariaDB {
    private final Logger logger;
    private final String mariadbHost;
    private final Integer mariadbPort;
    private final String mariadbUser;
    private final String mariadbPassword;
    private final String mariadbDatabase;
    private final String mariadbTablePrefix;

    public VelocityMariaDB(Logger logger, String mariadbHost, Integer mariadbPort, String mariadbUser, String mariadbPassword, String mariadbDatabase, String mariadbTablePrefix) {
        this.logger = logger;
        this.mariadbHost = mariadbHost;
        this.mariadbPort = mariadbPort;
        this.mariadbUser = mariadbUser;
        this.mariadbPassword = mariadbPassword;
        this.mariadbDatabase = mariadbDatabase;
        this.mariadbTablePrefix = mariadbTablePrefix;
    }

    public void setupDatabase() {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);

        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String createTempAccessTable = "CREATE TABLE IF NOT EXISTS " + mariadbTablePrefix + "temp_access (id INT AUTO_INCREMENT PRIMARY KEY, discord_id BIGINT NOT NULL, minecraft_username VARCHAR(255) NOT NULL, UNIQUE KEY (discord_id, minecraft_username))";
            try (PreparedStatement statement = connection.prepareStatement(createTempAccessTable)) {
                statement.execute();
                logger.info("Created temp_access table.");
            }

            String createWhitelistTable = "CREATE TABLE IF NOT EXISTS " + mariadbTablePrefix + "whitelist (id INT AUTO_INCREMENT PRIMARY KEY, discord_id BIGINT NOT NULL, minecraft_username VARCHAR(255) NOT NULL, bedrock_username VARCHAR(255) NOT NULL, UNIQUE KEY (discord_id, minecraft_username), UNIQUE KEY (discord_id, bedrock_username))";
            try (PreparedStatement statement = connection.prepareStatement(createWhitelistTable)) {
                statement.execute();
                logger.info("Created whitelist table.");
            }

            String createFailedAttemptsTable = "CREATE TABLE IF NOT EXISTS " + mariadbTablePrefix + "failed_attempts (discord_id BIGINT NOT NULL, attempts INT DEFAULT 0, PRIMARY KEY (discord_id))";
            try (PreparedStatement statement = connection.prepareStatement(createFailedAttemptsTable)) {
                statement.execute();
                logger.info("Created failed_attempts table.");
            }

            String createUserRewardsTable = "CREATE TABLE IF NOT EXISTS " + mariadbTablePrefix + "user_rewards (id INT AUTO_INCREMENT PRIMARY KEY, discord_id BIGINT NOT NULL, minecraft_uuid VARCHAR(255) NOT NULL, reward_type VARCHAR(255) NOT NULL, reward_count INT DEFAULT 0, last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY (discord_id, minecraft_uuid, reward_type))";
            try (PreparedStatement statement = connection.prepareStatement(createUserRewardsTable)) {
                statement.execute();
                logger.info("Created user_rewards table.");
            }

            logger.info("Database setup completed successfully.");
        } catch (SQLException e) {
            logger.error("Database setup error: ", e);
        }
    }

    public boolean isPlayerWhitelisted(String username) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);

        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "SELECT COUNT(*) FROM " + mariadbTablePrefix + "whitelist WHERE minecraft_username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() && resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while checking whitelist for player {}: {}", username, e.getMessage());
        }
        return false;
    }

    public void linkMinecraftAccount(String discordId, String minecraftUsername) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "INSERT INTO " + mariadbTablePrefix + "temp_access (discord_id, minecraft_username) VALUES (?, ?) ON DUPLICATE KEY UPDATE minecraft_username = VALUES(minecraft_username)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.setString(2, minecraftUsername);
                statement.executeUpdate();
                logger.info("Linked Discord ID " + discordId + " with Minecraft username " + minecraftUsername);
            }
        } catch (SQLException e) {
            logger.error("Database error while linking accounts", e);
        }
    }

    public void unlinkMinecraftAccount(String discordId) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "DELETE FROM " + mariadbTablePrefix + "temp_access WHERE discord_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.executeUpdate();
                logger.info("Unlinked Discord ID " + discordId + " from Minecraft account.");
            }
        } catch (SQLException e) {
            logger.error("Database error while unlinking accounts", e);
        }
    }

    public void incrementFailedAttempts(String discordId) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "INSERT INTO " + mariadbTablePrefix + "failed_attempts (discord_id, attempts) VALUES (?, 1) ON DUPLICATE KEY UPDATE attempts = attempts + 1";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.executeUpdate();
                logger.info("Incremented failed attempts for Discord ID " + discordId);
            }
        } catch (SQLException e) {
            logger.error("Database error while incrementing failed attempts", e);
        }
    }

    public String getDiscordIdByUsername(String minecraftUsername) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "SELECT discord_id FROM " + mariadbTablePrefix + "temp_access WHERE minecraft_username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, minecraftUsername);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString("discord_id");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while getting Discord ID by Minecraft username", e);
        }
        return null;
    }

    public void whitelistPlayer(String discordId, String minecraftUsername) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "INSERT INTO " + mariadbTablePrefix + "whitelist (discord_id, minecraft_username) VALUES (?, ?) ON DUPLICATE KEY UPDATE minecraft_username = VALUES(minecraft_username)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.setString(2, minecraftUsername);
                statement.executeUpdate();
                logger.info("Whitelisted Discord ID " + discordId + " with Minecraft username " + minecraftUsername);
            }
        } catch (SQLException e) {
            logger.error("Database error while whitelisting player", e);
        }
    }

    public void storeUserReward(String discordId, String minecraftUuid, String rewardType) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "INSERT INTO " + mariadbTablePrefix + "user_rewards (discord_id, minecraft_uuid, reward_type, reward_count) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE reward_count = reward_count + 1, last_updated = CURRENT_TIMESTAMP";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.setString(2, minecraftUuid);
                statement.setString(3, rewardType);
                statement.executeUpdate();
                logger.info("Stored reward for Discord ID " + discordId + " with Minecraft UUID " + minecraftUuid);
            }
        } catch (SQLException e) {
            logger.error("Database error while storing user reward", e);
        }
    }

    public List<Reward> getUserRewards(String discordId) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        List<Reward> rewards = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "SELECT reward_type, reward_count FROM " + mariadbTablePrefix + "user_rewards WHERE discord_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String rewardType = resultSet.getString("reward_type");
                        int rewardCount = resultSet.getInt("reward_count");
                        rewards.add(new Reward(rewardType, rewardCount));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while retrieving user rewards", e);
        }
        return rewards;
    }

    public void removeUserRewards(String discordId) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "DELETE FROM " + mariadbTablePrefix + "user_rewards WHERE discord_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, discordId);
                statement.executeUpdate();
                logger.info("Removed rewards for Discord ID " + discordId);
            }
        } catch (SQLException e) {
            logger.error("Database error while removing user rewards", e);
        }
    }

    public List<Reward> getCachedRewards(UUID uuid) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);
        List<Reward> rewards = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "SELECT * FROM " + mariadbTablePrefix + "cached_rewards WHERE minecraft_uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String rewardType = resultSet.getString("reward_type");
                        int rewardCount = resultSet.getInt("reward_count");
                        rewards.add(new Reward(rewardType, rewardCount));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while retrieving cached rewards", e);
        }
        return rewards;
    }

    public void cleanRewardCache(UUID uuid) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", mariadbHost, mariadbPort, mariadbDatabase);

        try (Connection connection = DriverManager.getConnection(url, mariadbUser, mariadbPassword)) {
            String query = "DELETE FROM " + mariadbTablePrefix + "cached_rewards WHERE minecraft_uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Database error while cleaning reward cache", e);
        }
    }

    private boolean isChumpCode(String value) {
        return value.equals("NeverGonnaGive") || value.equals("YouUpNever") || value.equals("GonnaLetYou") || value.equals("DownNeverGonna") ||
               value.equals("RunAroundAnd") || value.equals("DesertYouNever") || value.equals("GonnaMakeYou") || value.equals("CryNeverGonna") ||
               value.equals("SayGoodbyeNever") || value.equals("TellALie") || value.equals("AndHurtYou");
    }

    private void printChumpCodeMessage() {
        logger.error("chump code");
    }

    // Inner class to represent a Reward
    public static class Reward {
        private String rewardType;
        private int rewardCount;

        public Reward(String rewardType, int rewardCount) {
            this.rewardType = rewardType;
            this.rewardCount = rewardCount;
        }

        public String getRewardType() {
            return rewardType;
        }

        public int getRewardCount() {
            return rewardCount;
        }

        public void setRewardCount(int rewardCount) {
            this.rewardCount = rewardCount;
        }
    }
}