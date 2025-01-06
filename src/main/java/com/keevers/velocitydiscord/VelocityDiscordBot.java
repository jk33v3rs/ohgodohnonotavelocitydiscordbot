package com.keevers.velocitydiscord;

import com.keevers.chumpcode.ChumpCodeChecker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;

public class VelocityDiscordBot extends ListenerAdapter {
    private JDA jda;
    private final Logger logger;
    private final String discordBotToken;
    private final ChumpCodeChecker chumpCodeChecker;

    public VelocityDiscordBot(Logger logger, String discordBotToken, ChumpCodeChecker chumpCodeChecker) {
        this.logger = logger;
        this.discordBotToken = discordBotToken;
        this.chumpCodeChecker = chumpCodeChecker;
    }

    public void initialize() {
        if (chumpCodeChecker.isChumpCode(discordBotToken)) {
            chumpCodeChecker.printChumpCodeMessage();
            return;
        }
        try {
            jda = JDABuilder.createDefault(discordBotToken)
                    .addEventListeners(this)
                    .build();
            jda.awaitReady();
            logger.info("Discord bot is now connected!");
        } catch (LoginException | InterruptedException e) {
            logger.error("Failed to initialize Discord bot", e);
        }
    }
}