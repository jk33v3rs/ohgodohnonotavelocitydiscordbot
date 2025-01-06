package me.tini.discordrewards;

import me.tini.discordrewards.config.Config;
import me.tini.discordrewards.config.RewardManager;
import me.tini.discordrewards.linking.LinkedAccount;
import me.tini.discordrewards.util.CodeGenerator;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RewardsElf {

    private static final Logger logger = LoggerFactory.getLogger(RewardsElf.class);

    private final VelocityMariaDB database;
    private final RewardManager rewardManager;

    public RewardsElf(VelocityMariaDB database, RewardManager rewardManager) {
        this.database = database;
        this.rewardManager = rewardManager;
    }

    public void handleReward(LinkedAccount account, User user, MessageReceivedEvent event, Config config) {
        UUID uuid = UUID.fromString(account.getMinecraftUUID());
        int newMessageCount = account.getMessageCount() + 1;
        account.setMessageCount(newMessageCount);

        // Update the account message count in the database
        database.updateLinkedAccount(account);

        if (rewardManager.appliesForReward(newMessageCount)) {
            // Logic to give reward to the player
            rewardManager.giveReward(uuid, newMessageCount);

            if (rewardManager.shouldSendDiscordMessage()) {
                String message = config.getDiscordConfig().getReachedMessage()
                        .replace("{user_mention}", user.getAsMention())
                        .replace("{amount}", String.valueOf(newMessageCount))
                        .replace("{random_code}", generateRandomCode());

                event.getChannel().sendMessage(message).queue();
            }

            // Store the reward in the database
            database.storeUserReward(account.getDiscordId(), account.getMinecraftUUID(), "message_reward");
        }
    }

    public void handleReconnect(LinkedAccount account, User user) {
        UUID uuid = UUID.fromString(account.getMinecraftUUID());

        // Retrieve cached rewards and give them to the player
        for (RewardManager.Reward reward : rewardManager.getCachedRewards(uuid).toArray(RewardManager.Reward[]::new)) {
            rewardManager.give(reward, uuid);
        }

        // Clear the cache after giving rewards
        rewardManager.cleanCache(uuid);
    }

    public void handleGetCootiesCommand(UUID uuid) {
        // Cache rewards for the player
        rewardManager.cacheRewards(uuid);
    }

    private static String generateRandomCode() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedNow = now.format(formatter);
        return CodeGenerator.generateCode(8) + formattedNow;
    }
}