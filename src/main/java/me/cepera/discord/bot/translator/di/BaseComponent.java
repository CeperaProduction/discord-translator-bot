package me.cepera.discord.bot.translator.di;

import javax.inject.Singleton;

import dagger.Component;
import me.cepera.discord.bot.translator.discord.DiscordBot;

@Singleton
@Component(modules = {DiscordModule.class, ConfigModule.class, TranslateModule.class, DataModule.class})
public interface BaseComponent {

    DiscordBot getDiscordBot();

}
