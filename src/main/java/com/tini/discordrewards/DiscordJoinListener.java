package me.tini.discordrewards;

import me.tini.discordrewards.config.RewardManager;
import me.tini.discordrewards.config.RewardManager.Reward;
import me.tini.discordrewards.linking.LinkedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DiscordJoinListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordJoinListener.class);

    private final VelocityMariaDB database;
    private final RewardManager rewardManager;
    private final Gson gson;
    private final Map<UUID, String> pending;
    private final Map<Long, LinkedAccount> accounts;
    private final File linkedFile;
    private final Map<UUID, String> uuidNameCache;

    public DiscordJoinListener(VelocityMariaDB database, RewardManager rewardManager, File linkedFile) {
        this.database = database;
        this.rewardManager = rewardManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pending = new HashMap<>();
        this.accounts = new HashMap<>();
        this.uuidNameCache = new HashMap<>();
        this.linkedFile = linkedFile;

        loadAccountsFromFile();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        long discordId = member.getUser().getIdLong();

        LinkedAccount account = getAccountByDiscordId(discordId);

        if (account != null) {
            logger.info("A previously verified user has joined, reconnecting rewards for Discord ID " + discordId);
            handleReconnect(account);
        }
    }

    private void handleReconnect(LinkedAccount account) {
        UUID uuid = UUID.fromString(account.getUuid());

        // Retrieve cached rewards and give them to the player
        for (Reward reward : rewardManager.getCachedRewards(uuid).toArray(Reward[]::new)) {
            rewardManager.give(reward, uuid);
        }

        // Clear the cache after giving rewards
        rewardManager.cleanCache(uuid);
    }

    public void handleGetCootiesCommand(UUID uuid) {
        // Cache rewards for the player
        rewardManager.cacheRewards(uuid);
    }

    private void loadAccountsFromFile() {
        try {
            if (linkedFile.exists()) {
                LinkedAccount[] linked = gson.fromJson(new FileReader(linkedFile), LinkedAccount[].class);

                if (linked != null) {
                    for (LinkedAccount account : linked) {
                        accounts.put(account.getDiscordId(), account);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error loading accounts from file", e);
        }
    }

    private Map<Long, LinkedAccount> loadAccountsFromDatabase() {
        String url = String.format("jdbc:mariadb://%s:%d/%s", database.getMariadbHost(), database.getMariadbPort(), database.getMariadbDatabase());
        Map<Long, LinkedAccount> accounts = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(url, database.getMariadbUser(), database.getMariadbPassword())) {
            String query = "SELECT * FROM " + database.getMariadbTablePrefix() + "linked_accounts";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    long discordId = resultSet.getLong("discord_id");
                    String playerName = resultSet.getString("player_name");
                    String minecraftUUID = resultSet.getString("minecraft_uuid");
                    int messageCount = resultSet.getInt("message_count");

                    LinkedAccount account = new LinkedAccount(discordId, playerName, minecraftUUID, messageCount);
                    accounts.put(discordId, account);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while loading accounts", e);
        }
        return accounts;
    }

    private LinkedAccount getAccountByDiscordId(long discordId) {
        return accounts.get(discordId);
    }

    public void updateAccount(LinkedAccount account) {
        accounts.put(account.getId(), account);
        saveLinkedAccount(account);
    }

    public void saveLinkedAccount(LinkedAccount account) {
        String url = String.format("jdbc:mariadb://%s:%d/%s", database.getMariadbHost(), database.getMariadbPort(), database.getMariadbDatabase());

        try (Connection connection = DriverManager.getConnection(url, database.getMariadbUser(), database.getMariadbPassword())) {
            String query = "INSERT INTO " + database.getMariadbTablePrefix() + "linked_accounts (discord_id, player_name, minecraft_uuid, message_count) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), minecraft_uuid = VALUES(minecraft_uuid), message_count = VALUES(message_count)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, account.getId());
                statement.setString(2, account.getName());
                statement.setString(3, account.getUuid());
                statement.setInt(4, account.getMessageCount());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Database error while saving linked account", e);
        }
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public String generateCode() {
        String code;
        do {
            code = CodeGenerator.generateCode(8);
        } while (pending.values().contains(code));
        return code;
    }

    public LinkedAccount getAccount(Long id) {
        return accounts.get(id);
    }

    public boolean isLinked(Long id) {
        return accounts.get(id) != null;
    }

    public LinkedAccount getAccount(UUID uuid) {
        return accounts.values().stream()
                .filter(account -> account.getUuid().equals(uuid.toString()))
                .findFirst()
                .orElse(null);
    }

    public boolean isLinked(UUID uuid) {
        return accounts.values().stream()
                .map(LinkedAccount::getUuid)
                .anyMatch(uuid.toString()::equals);
    }

    public void addPendingPlayer(UUID uuid, String name, String code) {
        pending.put(uuid, code);
        uuidNameCache.put(uuid, name);
    }

    public void save() {
        if (accounts.values().size() == 0) {
            return;
        }
        try (OutputStream os = new FileOutputStream(linkedFile)) {
            String json = gson.toJson(accounts.values());
            os.write(json.getBytes());
            os.flush();
        } catch (IOException e) {
            logger.error("Error saving accounts to file", e);
        }
    }

    public boolean isValidCode(String code) {
        return pending.values().contains(code);
    }

    public LinkedAccount link(Long discordId, String code) {
        for (Map.Entry<UUID, String> entry : pending.entrySet()) {
            UUID uuid = entry.getKey();
            String validCode = entry.getValue();

            if (validCode.equals(code)) {
                LinkedAccount acc = new LinkedAccount(discordId, getName(uuid), uuid.toString(), 0);
                accounts.put(discordId, acc);

                // cleanup
                pending.values().remove(code);
                uuidNameCache.remove(uuid);

                save();

                return acc;
            }
        }
        return null;
    }

    private String getName(UUID uuid) {
        return uuidNameCache.get(uuid);
    }

    public Map<UUID, String> getPending() {
        return pending;
    }

    public Map<Long, LinkedAccount> getAccounts() {
        return accounts;
    }

    public Map<UUID, String> getUuidNameCache() {
        return uuidNameCache;
    }
}