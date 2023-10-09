package me.cepera.discord.bot.translator.discord;

import discord4j.core.DiscordClient;

public interface DiscordBot {

    void start();

    DiscordClient getDiscordClient();

}
