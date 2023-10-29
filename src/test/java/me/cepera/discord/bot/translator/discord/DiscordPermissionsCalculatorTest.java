package me.cepera.discord.bot.translator.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import discord4j.rest.util.Permission;

public class DiscordPermissionsCalculatorTest implements DiscordPermissionsCalculator{

    @Test
    void testPermissionBits() {

        String calculated = permissionBits(Permission.ADMINISTRATOR);

        assertEquals("8", calculated, "Calculated permission bits are bad");

    }

}
