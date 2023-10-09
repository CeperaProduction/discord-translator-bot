package me.cepera.discord.bot.translator.utils;

import java.util.Optional;

public class UnicodeUtils {

    public static Optional<String> unicodeFlagEmoji(String countryCode){
        if(countryCode.length() != 2) {
            return Optional.empty();
        }
        String upperCountryCode = countryCode.toUpperCase();
        int firstLetter = Character.codePointAt(upperCountryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(upperCountryCode, 1) - 0x41 + 0x1F1E6;
        return Optional.of(new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter)));
    }

}
