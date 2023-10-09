package me.cepera.discord.bot.translator.service;

import java.util.List;
import java.util.Optional;

import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TranslateService extends LocalService{

    default Flux<TranslatedText> translate(List<String> texts, TranslateLanguage targetLanguage){
        return translate(texts, targetLanguage, Optional.empty());
    }

    Flux<TranslatedText> translate(List<String> texts, TranslateLanguage targetLanguage,
            Optional<TranslateLanguage> sourceLanguage);

    Flux<TranslateLanguage> getAvailableLanguages();

    Flux<TranslateLanguage> getAvailableLanguages(List<String> firstLanguageIds);

    Mono<TranslateLanguage> getLanguage(String code);

}
