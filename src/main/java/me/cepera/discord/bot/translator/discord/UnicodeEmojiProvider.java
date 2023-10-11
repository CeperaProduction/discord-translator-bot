package me.cepera.discord.bot.translator.discord;

import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.utils.UnicodeUtils;
import reactor.core.publisher.Mono;

public interface UnicodeEmojiProvider {

    default Mono<String> unicodeFlagEmoji(TranslateLanguage lang){
        if(lang.getCountryCode() == null) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(UnicodeUtils.unicodeFlagEmoji(lang.getCountryCode()));
    }

}
