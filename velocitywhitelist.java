package com.example.velocitywhitelist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "ohgodohnonotavelocitydiscordpluginbot", name = "VelocityDiscordPluginBot", version = "1.0-SNAPSHOT", description = "A plugin to sync Discord roles with a Velocity whitelist", authors = {"YourName"})
public class VelocityWhitelist {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final LuckPerms luckPerms;
    private Map<String, Object> config;

    @Inject
    public VelocityWhitelist(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, LuckPerms luckPerms) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.luckPerms = luckPerms;
        createConfigFile();
        loadConfig();
        setupDatabase();
    }

    private void createConfigFile() {
        try {
            File configDir = dataDirectory.toFile();
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File configFile = new File(configDir, "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in == null) {
                        // Default config values
                        Map<String, Object> defaultConfig = new HashMap<>();
                        defaultConfig.put("mariadb", Map.of(
                                "host", "localhost",
                                "port", 3306,
                                "user", "user",
                                "password", "password",
                                "database", "database",
                                "table_prefix", "prefix_",
                                "ssl", false,
                                "public_key_retrieval", false
                        ));
                        defaultConfig.put("hooks", Map.of(
                                "use_luckperms", true,
                                "use_vault", false
                        ));
                        defaultConfig.put("roles", Map.of(
                                "temp_access", "temp access",
                                "certified_cool_kid", "certified cool kid",
                                "luckperms_temp_access", "temp_access",
                                "luckperms_certified_cool_kid", "certified_cool_kid"
                        ));
                        defaultConfig.put("discord", Map.of(
                                "bot_name", "VelocityBot",
                                "server_id", "YOUR_SERVER_ID",
                                "bot_token", "YOUR_BOT_TOKEN",
                                "listen_channels", List.of("channel_1", "channel_2"),
                                "ignore_channels", List.of("channel_3")
                        ));
                        defaultConfig.put("commands", Map.of(
                                "give_cool_kid_juice", "/give cool kid juice to <minecraftusername>",
                                "cool_kid_message", "<minecraftusername> got some cool kid juice. How is it?",
                                "weird_response", "weird",
                                "required_role", "cool kid"
                        ));
                        defaultConfig.put("geyser", Map.of(
                                "prefix", "."
                        ));
                        defaultConfig.put("startups", 0);

                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        Yaml yaml = new Yaml(options);
                        try (FileWriter writer = new FileWriter(configFile)) {
                            yaml.dump(defaultConfig, writer);
                        }
                    } else {
                        Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create default configuration", e);
        }
    }

    private void loadConfig() {
        try {
            File configFile = new File(dataDirectory.toFile(), "config.yml");
            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(configFile.toPath())) {
                config = yaml.load(in);
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
        }
    }

    private void setupDatabase() {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mariadb://" + config.get("mariadb.host") + ":" + config.get("mariadb.port") + "/" + config.get("mariadb.database"),
                (String) config.get("mariadb.user"),
                (String) config.get("mariadb.password"))) {
            String tablePrefix = (String) config.get("mariadb.table_prefix");

            // Create temp_access table if not exists
            String createTempAccessTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "temp_access ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "discord_id BIGINT NOT NULL, "
                    + "minecraft_username VARCHAR(255) NOT NULL, "
                    + "UNIQUE KEY (discord_id, minecraft_username))";
            try (PreparedStatement statement = connection.prepareStatement(createTempAccessTable)) {
                statement.execute();
            }

            // Create whitelist table if not exists
            String createWhitelistTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "whitelist ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "discord_id BIGINT NOT NULL, "
                    + "minecraft_username VARCHAR(255) NOT NULL, "
                    + "bedrock_username VARCHAR(255) NOT NULL, "
                    + "UNIQUE KEY (discord_id, minecraft_username), "
                    + "UNIQUE KEY (discord_id, bedrock_username))";
            try (PreparedStatement statement = connection.prepareStatement(createWhitelistTable)) {
                statement.execute();
            }

            logger.info("Database setup completed successfully.");
        } catch (SQLException e) {
            logger.error("Database setup error", e);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        String geyserPrefix = (String) config.get("geyser.prefix");
        String bedrockUsername = geyserPrefix + username;

        if (!isPlayerWhitelisted(username) && !isPlayerWhitelisted(bedrockUsername)) {
            // Assign default role and set adventure mode
            assignRole(player, (String) config.get("roles.temp_access"));
            // Send player to HUB in adventure mode
            sendToHubAdventureMode(player);
        } else {
            // Assign elevated role for whitelisted players
            assignRole(player, (String) config.get("roles.certified_cool_kid"));
        }
    }

    private boolean isPlayerWhitelisted(String username) {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mariadb://" + config.get("mariadb.host") + ":" + config.get("mariadb.port") + "/" + config.get("mariadb.database"),
                (String) config.get("mariadb.user"),
                (String) config.get("mariadb.password"))) {
            String query = "SELECT COUNT(*) FROM " + config.get("mariadb.table_prefix") + "whitelist WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while checking whitelist for player {}: {}", username, e.getMessage());
        }
        return false;
    }

    private void assignRole(Player player, String role) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().add(Node.builder("group." + role).build());
            luckPerms.getUserManager().saveUser(user);
        }
    }

    private void sendToHubAdventureMode(Player player) {
        // Implement this function to send player to the HUB server in adventure mode
    }
}