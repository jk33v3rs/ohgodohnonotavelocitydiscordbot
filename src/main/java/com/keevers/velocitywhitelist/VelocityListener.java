package com.keevers.velocitywhitelist;

import com.keevers.logging.CustomLogger;
import com.keevers.velocitydiscord.VelocityDiscordBot;
import com.keevers.velocitymariadb.VelocityMariaDB;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class VelocityListener extends ListenerAdapter {
    private final ProxyServer server;
    private final LuckPerms luckPerms;
    private final VelocityMariaDB mariaDB;
    private final VelocityDiscordBot discordBot;
    private final String tagPrefix;
    private final String giveCoolKidJuiceCommand;
    private final String weirdResponse;

    public VelocityListener(ProxyServer server, VelocityMariaDB mariaDB, VelocityDiscordBot discordBot, String tagPrefix, String giveCoolKidJuiceCommand, String weirdResponse) {
        this.server = server;
        this.luckPerms = LuckPermsProvider.get();
        this.mariaDB = mariaDB;
        this.discordBot = discordBot;
        this.tagPrefix = tagPrefix;
        this.giveCoolKidJuiceCommand = giveCoolKidJuiceCommand;
        this.weirdResponse = weirdResponse;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        String bedrockUsername = discordBot.getGeyserPrefix() + username;

        if (!mariaDB.isPlayerWhitelisted(username) && !mariaDB.isPlayerWhitelisted(bedrockUsername)) {
            sendToHubAdventureMode(player);
        } else {
            assignRole(player, discordBot.getRolesCertifiedCoolKid());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (message.startsWith(tagPrefix + giveCoolKidJuiceCommand)) {
            String[] parts = message.split(" ");
            if (parts.length != 5 || !parts[3].equalsIgnoreCase("to")) {
                event.getChannel().sendMessage("Usage: " + tagPrefix + giveCoolKidJuiceCommand + " <MinecraftUsername>").queue();
                return;
            }
            String minecraftUsername = parts[4];
            String discordId = event.getAuthor().getId();

            // Check if the user has the required role to trigger the command
            if (event.getMember() != null && event.getMember().getRoles().stream().anyMatch(role -> role.getName().equals(discordBot.getRolesTempAccess()))) {
                mariaDB.linkMinecraftAccount(discordId, minecraftUsername);
                assignTempAccessRole(discordId, minecraftUsername);
                event.getChannel().sendMessage("Temporary access granted to " + minecraftUsername + ". You have 5 minutes to join and say \"" + weirdResponse + "\".").queue();
            } else {
                event.getChannel().sendMessage("You do not have the required role to use this command.").queue();
            }
        }
    }

    private void assignRole(Player player, String role) {
        if (luckPerms == null) {
            CustomLogger.getLogger().severe("LuckPerms API is not loaded, cannot assign role!");
            return;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().add(Node.builder("group." + role).build());
            luckPerms.getUserManager().saveUser(user);
            CustomLogger.getLogger().info("Assigned role " + role + " to player " + player.getUsername());
        }
    }

    private void assignTempAccessRole(String discordId, String minecraftUsername) {
        // Logic to assign the temp access role to the player and start the timer
        Player player = server.getPlayer(minecraftUsername).orElse(null);
        if (player != null) {
            assignRole(player, discordBot.getRolesTempAccess());
            startTemporaryAccessTimer(player, discordId);
        } else {
            CustomLogger.getLogger().info("Player " + minecraftUsername + " is not online.");
        }
    }

    private void sendToHubAdventureMode(Player player) {
        CustomLogger.getLogger().info("Sending player " + player.getUsername() + " to HUB in adventure mode");
        // Implement logic to send player to hub in adventure mode
    }

    private void startTemporaryAccessTimer(Player player, String discordId) {
        server.getScheduler().buildTask(server, () -> {
            if (!mariaDB.isPlayerWhitelisted(player.getUsername())) {
                removeTemporaryAccess(player, discordId);
                // Increment failed attempts counter in the database
                mariaDB.incrementFailedAttempts(discordId);
            }
        }).delay(5, TimeUnit.MINUTES).schedule();
        CustomLogger.getLogger().info("Started 5-minute temporary access timer for player " + player.getUsername());
    }

    private void removeTemporaryAccess(Player player, String discordId) {
        if (luckPerms == null) {
            CustomLogger.getLogger().severe("LuckPerms API is not loaded, cannot remove temporary access!");
            return;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().remove(Node.builder("group." + discordBot.getRolesTempAccess()).build());
            luckPerms.getUserManager().saveUser(user);
            CustomLogger.getLogger().info("Removed temporary access role from player " + player.getUsername());
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (event.getMessage().equalsIgnoreCase(weirdResponse)) {
            String username = player.getUsername();
            String discordId = mariaDB.getDiscordIdByUsername(username);
            if (discordId != null) {
                mariaDB.whitelistPlayer(discordId, username);
                assignRole(player, discordBot.getRolesCertifiedCoolKid());
                CustomLogger.getLogger().info("Player " + username + " has been permanently whitelisted.");
            }
        }
    }
}