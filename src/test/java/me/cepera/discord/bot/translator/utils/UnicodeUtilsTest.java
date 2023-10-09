package me.cepera.discord.bot.translator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class UnicodeUtilsTest {

    @Test
    void testCountryFlagEmojiCalculation() {

        Optional<String> optEmoji = UnicodeUtils.unicodeFlagEmoji("fr");
        String expected = "\uD83C\uDDEB\uD83C\uDDF7";

        assertTrue(optEmoji.isPresent(), "Emoji was not generated");
        assertEquals(expected, optEmoji.get(), "Generated bad emoji");

    }

}
