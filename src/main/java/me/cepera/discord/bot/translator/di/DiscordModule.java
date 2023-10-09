package me.cepera.discord.bot.translator.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.cepera.discord.bot.translator.discord.DiscordBot;
import me.cepera.discord.bot.translator.discord.DiscordTranslatorBot;

@Module
public class DiscordModule {

    @Provides
    @Singleton
    DiscordBot discordBot(DiscordTranslatorBot translatorBot) {
        return translatorBot;
    }

}
