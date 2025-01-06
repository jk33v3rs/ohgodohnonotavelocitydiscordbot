package com.tini.discordrewards;

import com.tini.discordrewards.config.Config;
import com.tini.discordrewards.config.RewardManager;
import com.tini.discordrewards.linking.LinkManager;
import com.tini.discordrewards.linking.LinkedAccount;
import com.tini.discordrewards.linking.LinkingServiceImpl;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class DiscordRewards {

    private final LinkManager linkManager;
    private final Config config;
    private final RewardManager rewardManager;
    private final LinkingServiceImpl linkingService;

    private GuildMessageChannel channel;
    private Long channelId;

    public DiscordRewards(Config config, LinkManager linkManager, RewardManager rewardManager, LinkingServiceImpl linkingService) {
        this.config = config;
        this.linkManager = linkManager;
        this.rewardManager = rewardManager;
        this.linkingService = linkingService;
    }

    public void initialize() {
        this.channelId = config.getDiscordConfig().getChannelId();
        DiscordBot bot = getBotInstance();
        this.channel = bot.getJda().getTextChannelById(channelId);

        if (channel == null) {
            System.err.println("===================================================");
            System.err.println("Channel with id '" + channelId + "' was not found.");
            System.err.println("The linking system is now disabled.");
            System.err.println("===================================================");
            return;
        }

        this.linkManager.setBot(bot);
        this.linkManager.setGuild(channel.getGuild());
        this.linkManager.setDiscordConfig(config.getDiscordConfig());

        bot.getJda().addEventListener(new DiscordJoinListener(linkManager, this));

        Guild guild = channel.getGuild();

        // Register slash commands
        registerCommands(bot, guild);
    }

    private void registerCommands(DiscordBot bot, Guild guild) {
        bot.getJda().upsertCommand("instructions", "Send the linking instructions")
            .setDefaultPermissions(Permission.MANAGE_CHANNEL)
            .queue();

        bot.getJda().upsertCommand("getcooties", "Unlink your Minecraft account from your Discord account")
            .setDefaultPermissions(Permission.MANAGE_CHANNEL)
            .queue();

        bot.getJda().addEventListener(new SlashCommandInteractionEvent(this::handleInstructionsCommand));
        bot.getJda().addEventListener(new SlashCommandInteractionEvent(this::handleUnlinkCommand));
    }

    private void handleInstructionsCommand(SlashCommandInteractionEvent event) {
        MessageEmbed embed = config.getDiscordConfig().getInstructionsEmbed();
        event.replyEmbeds(embed).queue();
    }

    private void handleUnlinkCommand(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();
        LinkedAccount account = linkManager.getAccount(userId);

        if (account != null) {
            linkManager.unlinkAccount(userId);
            event.reply("Your Minecraft account has been unlinked from your Discord account.").queue();
        } else {
            event.reply("No linked account found.").queue();
        }
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (config.isRewardEnabled()) {
            User user = event.getAuthor();
            long userId = user.getIdLong();
            LinkedAccount account = linkManager.getAccount(userId);

            if (account != null) {
                RewardsElf.handleReward(account, user, event, rewardManager, config);
            }
        }
    }

    private DiscordBot getBotInstance() {
        // Implement this method to return your DiscordBot instance
        return null;
    }
}