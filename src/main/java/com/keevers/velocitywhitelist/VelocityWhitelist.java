package com.keevers.velocitywhitelist;

import com.keevers.chumpcode.ChumpCodeChecker;
import com.keevers.logging.CustomLogger;
import com.keevers.velocitydiscordbot.VelocityDiscordBot;
import com.keevers.velocitymariadb.VelocityMariaDB;
import com.tini.discordrewards.RewardsElf;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(
        id = "velocity-whitelist",
        name = "VelocityWhitelist",
        version = "1.9.0",
        description = "A plugin to manage Velocity whitelist configuration.",
        authors = {"jk33v3rs"}
)
public class VelocityWhitelist {
    private final ProxyServer server;
    private final CustomLogger logger;
    private final Path dataDirectory;
    private boolean configLoaded = false;

    private String mariadbHost;
    private Integer mariadbPort;
    private String mariadbUser;
    private String mariadbPassword;
    private String mariadbDatabase;
    private String mariadbTablePrefix;
    private String discordBotToken;
    private String geyserPrefix;
    private String rolesTempAccess;
    private String rolesCertifiedCoolKid;

    private LuckPerms luckPerms;
    private VelocityDiscordBot discordBot;
    private VelocityMariaDB mariaDB;
    private ChumpCodeChecker chumpCodeChecker;

    @Inject
    public VelocityWhitelist(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = CustomLogger.getLogger();
        this.dataDirectory = dataDirectory.resolve("ohnovelocitywhitelist");
        CustomLogger.setup(this.dataDirectory);
        this.chumpCodeChecker = new ChumpCodeChecker();
        this.configLoaded = ensureConfigLoaded();
        if (!configLoaded) {
            this.logger.severe("Failed to load configuration. Plugin functionality is disabled.");
        } else {
            try {
                Map<String, Object> config = loadConfig(dataDirectory.resolve("config.yml"));
                this.mariaDB = new VelocityMariaDB(logger, mariadbHost, mariadbPort, mariadbUser, mariadbPassword, mariadbDatabase, mariadbTablePrefix);
                this.discordBot = new VelocityDiscordBot(mariaDB, new RewardsElf(logger), config);
            } catch (LoginException e) {
                logger.error("Failed to initialize Discord bot", e);
            }
        }
    }

    private boolean ensureConfigLoaded() {
        try {
            Path configFile = dataDirectory.resolve("config.yml");

            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                logger.info("Created directory: " + dataDirectory);
                pause(15000); // Pause for 15 seconds if directory is created
            }

            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
                logger.info("Created default config file: " + configFile);
                pause(15000); // Pause for 15 seconds if config file is created
            }

            // Load the config file
            loadConfig(configFile);

            return true;
        } catch (Exception e) {
            logger.severe("Failed to ensure configuration is loaded: " + e.getMessage());
            return false;
        }
    }

    private void createDefaultConfig(Path configFile) {
        try {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("mariadb", Map.of("host", "example.com", "port", 3306, "user", "username", "password", "password", "database", "database_name", "table_prefix", "prefix_"));
            defaultConfig.put("hooks", Map.of("use_luckperms", true, "use_vault", false));
            defaultConfig.put("roles", Map.of("temp_access", "temp_role", "certified_cool_kid", "cool_kid_role"));
            defaultConfig.put("discord", Map.of("bot_name", "Bot Name", "server_id", "server_id", "bot_token", "your_bot_token", "listen_channels", List.of("channel_1", "channel_2"), "ignore_channels", List.of("channel_3")));
            defaultConfig.put("commands", Map.of("give_cool_kid_juice", "/give cool kid juice to <minecraftusername>", "cool_kid_message", "<minecraftusername> got some cool kid juice. How is it?"));
            defaultConfig.put("geyser", Map.of("prefix", "."));
            defaultConfig.put("startups", 0);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                yaml.dump(defaultConfig, writer);
            }
        } catch (Exception e) {
            logger.severe("Failed to create default configuration: " + e.getMessage());
        }
    }

    private Map<String, Object> loadConfig(Path configFile) {
        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(in);

            Map<String, Object> mariadbConfig = (Map<String, Object>) config.get("mariadb");
            mariadbHost = (String) mariadbConfig.get("host");
            mariadbPort = (Integer) mariadbConfig.get("port");
            mariadbUser = (String) mariadbConfig.get("user");
            mariadbPassword = (String) mariadbConfig.get("password");
            mariadbDatabase = (String) mariadbConfig.get("database");
            mariadbTablePrefix = (String) mariadbConfig.get("table_prefix");

            Map<String, Object> discordConfig = (Map<String, Object>) config.get("discord");
            discordBotToken = (String) discordConfig.get("bot_token");

            Map<String, Object> geyserConfig = (Map<String, Object>) config.get("geyser");
            geyserPrefix = (String) geyserConfig.get("prefix");

            Map<String, Object> rolesConfig = (Map<String, Object>) config.get("roles");
            rolesTempAccess = (String) rolesConfig.get("temp_access");
            rolesCertifiedCoolKid = (String) rolesConfig.get("certified_cool_kid");

            logger.info("Configuration loaded successfully.");
            // Log the loaded configuration values
            logger.info("MariaDB Host: " + mariadbHost);
            logger.info("MariaDB Port: " + mariadbPort);
            logger.info("MariaDB User: " + mariadbUser);
            logger.info("Discord Bot Token: " + discordBotToken);
            logger.info("Geyser Prefix: " + geyserPrefix);
            logger.info("Roles Temp Access: " + rolesTempAccess);
            logger.info("Roles Certified Cool Kid: " + rolesCertifiedCoolKid);

            return config;
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            return null;
        }
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (configLoaded) {
            mariaDB.setupDatabase();
            initializeLuckPerms();
            discordBot.initialize();

            // Register the new listener
            VelocityListener listener = new VelocityListener(server, mariaDB, discordBot, geyserPrefix, rolesTempAccess, rolesCertifiedCoolKid);
            server.getEventManager().register(this, listener);
            discordBot.getJDA().addEventListener(listener);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (configLoaded) {
            Player player = event.getPlayer();
            String username = player.getUsername();
            String bedrockUsername = geyserPrefix + username;
            if (!mariaDB.isPlayerWhitelisted(username) && !mariaDB.isPlayerWhitelisted(bedrockUsername)) {
                assignRole(player, rolesTempAccess);
                sendToHubAdventureMode(player);
            } else {
                assignRole(player, rolesCertifiedCoolKid);
            }
        }
    }

    private void assignRole(Player player, String role) {
        if (luckPerms == null) {
            logger.severe("LuckPerms API is not loaded, cannot assign role!");
            return;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().add(Node.builder("group." + role).build());
            luckPerms.getUserManager().saveUser(user);
            logger.info("Assigned role " + role + " to player " + player.getUsername());
        }
    }

    private void sendToHubAdventureMode(Player player) {
        logger.info("Sending player " + player.getUsername() + " to HUB in adventure mode");
        // ... (Logic to send player to hub)
    }

    private void initializeLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            logger.severe("LuckPerms API is not loaded: " + e.getMessage());
        }
    }

    private void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Thread sleep interrupted: " + e.getMessage());
        }
    }
}