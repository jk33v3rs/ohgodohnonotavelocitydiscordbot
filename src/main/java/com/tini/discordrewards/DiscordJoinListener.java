package com.tini.discordrewards;

import com.keevers.velocitymariadb.VelocityMariaDB;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import javax.security.auth.login.LoginException;
import java.util.Map;

public class DiscordJoinListener extends ListenerAdapter {
    private final Logger logger;
    private final VelocityMariaDB database;
    private final String botToken;
    private final JDA jda;
    private final String geyserPrefix;
    private final String rolesCertifiedCoolKid;
    private final String rolesTempAccess;
    private final String giveCoolKidJuiceCommand;
    private final String weirdResponse;
    private final String certifiedCoolKidRole;

    public DiscordJoinListener(Logger logger, VelocityMariaDB database, Map<String, Object> config) throws LoginException {
        this.logger = logger;
        this.database = database;
        this.botToken = (String) config.get("bot_token");
        this.geyserPrefix = (String) config.get("geyser_prefix");
        this.rolesCertifiedCoolKid = (String) config.get("roles_certified_cool_kid");
        this.rolesTempAccess = (String) config.get("roles_temp_access");
        
        // Load commands and responses from config
        this.giveCoolKidJuiceCommand = (String) config.get("give_cool_kid_juice");
        this.weirdResponse = (String) config.get("weird_response");
        this.certifiedCoolKidRole = (String) config.get("certified_cool_kid_role");

        this.jda = JDABuilder.createDefault(botToken).build();
        this.jda.addEventListener(this);
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

            logger.info("Linked Discord ID {} with Minecraft username {}", discordId, minecraftUsername);
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
        // This is a placeholder - actual implementation will depend on your specific setup
    }

    public void unlinkAccount(long discordId) {
        database.unlinkMinecraftAccount(String.valueOf(discordId));
    }
}