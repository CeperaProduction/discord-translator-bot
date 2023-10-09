package me.cepera.discord.bot.translator.repository;

import reactor.core.publisher.Mono;

public interface TranslateLanguageUsageRepository {

    Mono<String> getLastLanguageCode(long userId);

    Mono<Void> setLastLanguageCode(long userId, String languageCode);

}
