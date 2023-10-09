package me.cepera.discord.bot.translator.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.cepera.discord.bot.translator.config.ConfigReader;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import me.cepera.discord.bot.translator.config.TranslatorConfig;

@Module
public class ConfigModule {

    @Provides
    @Singleton
    ConfigReader configReader() {
        return new ConfigReader();
    }

    @Provides
    @Singleton
    DiscordBotConfig discordBotConfig(ConfigReader reader) {
        return reader.readConfig("discord", DiscordBotConfig.class);
    }

    @Provides
    @Singleton
    TranslatorConfig yandexTranslatorConfig(ConfigReader reader) {
        return reader.readConfig("translator", TranslatorConfig.class);
    }

}
