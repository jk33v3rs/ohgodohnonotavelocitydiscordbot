package com.keevers.velocitydiscordbot;

import com.keevers.logging.CustomLogger;
import com.keevers.velocitymariadb.VelocityMariaDB;
import com.tini.discordrewards.RewardsElf;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.Map;

public class VelocityDiscordBot extends ListenerAdapter { 
    private static final CustomLogger logger = CustomLogger.getLogger();
    private final VelocityMariaDB database;
    private final JDA jda;
    private final String geyserPrefix;
    private final String rolesCertifiedCoolKid;
    private final String rolesTempAccess;
    private final String giveCoolKidJuiceCommand;
    private final String weirdResponse;
    private final String certifiedCoolKidRole;
    private final RewardsElf rewardsElf;
    private final String botToken;

    public VelocityDiscordBot(VelocityMariaDB database, RewardsElf rewardsElf, Map<String, Object> config) throws LoginException {
        this.database = database;
        this.rewardsElf = rewardsElf;
        this.geyserPrefix = (String) config.get("geyser_prefix");
        this.rolesCertifiedCoolKid = (String) config.get("roles_certified_cool_kid");
        this.rolesTempAccess = (String) config.get("roles_temp_access");
        this.giveCoolKidJuiceCommand = (String) config.get("give_cool_kid_juice");
        this.weirdResponse = (String) config.get("weird_response");
        this.certifiedCoolKidRole = (String) config.get("certified_cool_kid_role");
        this.botToken = (String) config.get("bot_token");
        this.jda = JDABuilder.createDefault(botToken).build();
        this.jda.addEventListener(this);
    }

    public void initialize() {
        try {
            jda.awaitReady();
            logger.info("Discord bot initialized successfully.");
        } catch (InterruptedException e) {
            logger.error("Failed to initialize Discord bot", e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] parts = message.split(" ");

        if (parts.length > 4 && parts[0].equalsIgnoreCase("/give") && parts[1].equalsIgnoreCase("cool") && parts[2].equalsIgnoreCase("kid") && parts[3].equalsIgnoreCase("juice")) {
            String minecraftUsername = parts[4];
            long discordId = event.getAuthor().getIdLong();

            // Link the account in the database
            linkAccount(discordId, minecraftUsername);

            // Give temporary access role
            giveTempAccess(discordId);

            logger.info("Linked Discord ID " + discordId + " with Minecraft username " + minecraftUsername);
        }
    }

    public JDA getJDA() {
        return jda;
    }

    private void linkAccount(long discordId, String minecraftUsername) {
        database.linkMinecraftAccount(String.valueOf(discordId), minecraftUsername);
    }

    private void giveTempAccess(long discordId) {
        // Implement logic to give temporary access role
    }

    public void unlinkAccount(long discordId) {
        database.unlinkMinecraftAccount(String.valueOf(discordId));
    }

    // Getters for all fields
    public CustomLogger getLogger() {
        return logger;
    }

    public VelocityMariaDB getDatabase() {
        return database;
    }

    public String getGeyserPrefix() {
        return geyserPrefix;
    }

    public String getRolesCertifiedCoolKid() {
        return rolesCertifiedCoolKid;
    }

    public String getRolesTempAccess() {
        return rolesTempAccess;
    }

    public String getGiveCoolKidJuiceCommand() {
        return giveCoolKidJuiceCommand;
    }

    public String getWeirdResponse() {
        return weirdResponse;
    }

    public String getCertifiedCoolKidRole() {
        return certifiedCoolKidRole;
    }

    public RewardsElf getRewardsElf() {
        return rewardsElf;
    }

    public String getBotToken() {
        return botToken;
    }
}