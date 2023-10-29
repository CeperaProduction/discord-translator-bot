package me.cepera.discord.bot.translator.discord;

import discord4j.rest.util.Permission;

public interface DiscordPermissionsCalculator {

    default String permissionBits(Permission... permissions) {
        long summary = 0;
        for(Permission permission : permissions) {
            summary |= permission.getValue();
        }
        return Long.toHexString(summary);
    }

}
