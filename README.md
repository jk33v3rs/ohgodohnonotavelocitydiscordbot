### `README.md`

# ohgodohnonotavelocitydiscordpluginbot

A Velocity plugin that integrates with Discord to manage whitelisting for Minecraft servers connected through Velocity. This plugin allows players to gain access to the server by interacting with a Discord bot.

## Features

- Syncs Discord roles with LuckPerms roles in Minecraft.
- Manages whitelisting for all servers behind Velocity.
- Allows non-whitelisted players to join a HUB server in adventure mode.
- Customizable messages and commands.
- Supports Geyser prefixes for Bedrock players.

## Prerequisites

- Java 17 or higher
- Velocity 3.1.1 or higher
- LuckPerms plugin (optional)
- MariaDB server
- Discord bot token

## Configuration

The plugin will automatically generate a `config.yml` file on the first boot. This file will contain all the necessary configurations that you can customize.

### Example `config.yml`

```yaml
mariadb:
  host: "localhost"
  port: 3306
  user: "user"
  password: "password"
  database: "database"
  table_prefix: "prefix_"
  ssl: false
  public_key_retrieval: false

hooks:
  use_luckperms: true
  use_vault: false

roles:
  temp_access: "temp access"
  certified_cool_kid: "certified cool kid"
  luckperms_temp_access: "temp_access"
  luckperms_certified_cool_kid: "certified_cool_kid"

discord:
  bot_name: "VelocityBot"
  server_id: "YOUR_SERVER_ID"
  bot_token: "YOUR_BOT_TOKEN"
  listen_channels:
    - "channel_1"
    - "channel_2"
  ignore_channels:
    - "channel_3"

commands:
  give_cool_kid_juice: "/give cool kid juice to <minecraftusername>"
  cool_kid_message: "<minecraftusername> got some cool kid juice. How is it?"
  weird_response: "weird"
  required_role: "cool kid"

geyser:
  prefix: "."

startups: 0
```

## How to Use

### 1. Set Up the Discord Bot

1. Create a new Discord bot on the [Discord Developer Portal](https://discord.com/developers/applications).
2. Copy the bot token and add it to the `config.yml` file under `discord.bot_token`.

### 2. Configure MariaDB

1. Set up a MariaDB server.
2. The plugin will automatically create the necessary tables (`temp_access` and `whitelist`) if they do not exist.

### 3. Build and Deploy the Plugin

#### Clone the Repository

```bash
git clone https://github.com/jk33v3rs/ohgodohnonotavelocitydiscordpluginbot.git
cd ohgodohnonotavelocitydiscordpluginbot
```

#### Build the Plugin Using Maven

```bash
mvn clean package
```

#### Copy the JAR File

Copy the generated JAR file from the `target` directory to the `plugins` directory of your Velocity server.

### 4. Start the Velocity Server

Start your Velocity server. On the first boot, the plugin will create the `config.yml` file and the necessary database tables if they do not exist.

### 5. Use the Discord Command

In Discord, use the command `/give cool kid juice to <minecraftusername>` to initiate the whitelisting process.

## How to Contribute

### Fork the Repository

Click the "Fork" button on the top right of the repository page on GitHub to create a copy of the repository on your GitHub account.

### Clone Your Fork

```bash
git clone https://github.com/yourusername/ohgodohnonotavelocitydiscordpluginbot.git
cd ohgodohnonotavelocitydiscordpluginbot
```

### Create a Branch

```bash
git checkout -b my-feature-branch
```

### Make Your Changes

Make the necessary changes to the codebase.

### Commit and Push

```bash
git add .
git commit -m "Add my new feature"
git push origin my-feature-branch
```

### Create a Pull Request

Go to the original repository and click on the "Pull Requests" tab. Click the "New Pull Request" button and select your branch from the dropdown.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Support

If you have any questions or need help, feel free to open an issue in the repository.

---
