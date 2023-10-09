package me.cepera.discord.bot.translator;

import me.cepera.discord.bot.translator.di.DaggerBaseComponent;

public class Application {

    public static void main(String[] args) {

        DaggerBaseComponent.create().getDiscordBot().start();

    }

}
