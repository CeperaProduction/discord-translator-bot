package me.cepera.discord.bot.translator.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.cepera.discord.bot.translator.converter.BodyConverter;
import me.cepera.discord.bot.translator.dto.yandex.YandexLanguage;
import me.cepera.discord.bot.translator.dto.yandex.YandexListLanguagesResponse;
import me.cepera.discord.bot.translator.dto.yandex.YandexTranslateRequest;
import me.cepera.discord.bot.translator.dto.yandex.YandexTranslateResponse;
import me.cepera.discord.bot.translator.dto.yandex.YandexTranslation;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.service.TranslateService;
import me.cepera.discord.bot.translator.service.remote.YandexTranslatorRemoteService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class YandexTranslateService implements TranslateService{

    private static final Logger LOGGER = LogManager.getLogger(YandexTranslateService.class);

    private final YandexTranslatorRemoteService remote;

    private final BodyConverter<YandexTranslateRequest> translateRequestConverter;

    private final BodyConverter<YandexTranslateResponse> translateResponseConverter;

    private final BodyConverter<YandexListLanguagesResponse> listLanguagesResponseConverter;

    private List<TranslateLanguage> languages = Collections.emptyList();

    @Inject
    public YandexTranslateService(YandexTranslatorRemoteService remote,
            BodyConverter<YandexTranslateRequest> translateRequestConverter,
            BodyConverter<YandexTranslateResponse> translateResponseConverter,
            BodyConverter<YandexListLanguagesResponse> listLanguagesResponseConverter) {
        this.remote = remote;
        this.translateRequestConverter = translateRequestConverter;
        this.translateResponseConverter = translateResponseConverter;
        this.listLanguagesResponseConverter = listLanguagesResponseConverter;

        fetchLanguages().subscribe(this::setLanguages);

    }

    private void setLanguages(List<TranslateLanguage> languages) {
        List<TranslateLanguage> sortedLanguages = new ArrayList<>(languages);
        Collections.sort(sortedLanguages, (l1, l2)->l1.getCode().compareTo(l2.getCode()));
        this.languages = sortedLanguages;
        LOGGER.info("Loaded languages: {}", this.languages);
    }

    @Override
    public Flux<TranslatedText> translate(List<String> texts, TranslateLanguage targetLanguage,
            Optional<TranslateLanguage> sourceLanguage) {

        YandexTranslateRequest request = new YandexTranslateRequest();
        request.setTexts(texts);
        request.setTargetLanguageCode(targetLanguage.getCode());
        request.setFormat("PLAIN_TEXT");
        request.setSpeller(true);
        sourceLanguage.ifPresent(sourceLang->request.setSourceLanguageCode(getYandexLanguageCode(sourceLang)));

        return translateRequestConverter.write(request)
                .flatMap(remote::translate)
                .flatMap(translateResponseConverter::read)
                .flatMapIterable(YandexTranslateResponse::getTranslations)
                .map(this::mapText);
    }

    @Override
    public Flux<TranslateLanguage> getAvailableLanguages() {
        return Flux.fromIterable(languages);
    }

    @Override
    public Flux<TranslateLanguage> getAvailableLanguages(List<String> firstLanguageIds) {
        List<TranslateLanguage> result = new ArrayList<TranslateLanguage>();
        firstLanguageIds.forEach(id->languages.stream()
                .filter(lang->lang.getCode().equalsIgnoreCase(id))
                .findAny()
                .ifPresent(result::add));
        result.addAll(languages);
        return Flux.fromIterable(result);
    }

    @Override
    public Mono<TranslateLanguage> getLanguage(String code) {
        return getAvailableLanguages()
                .filter(lang->lang.getCode().equalsIgnoreCase(code))
                .take(1)
                .singleOrEmpty()
                .switchIfEmpty(Mono.fromSupplier(()->new TranslateLanguage(code, code, null)));
    }

    private String getYandexLanguageCode(TranslateLanguage language) {
        return language.getCode();
    }

    private Mono<List<TranslateLanguage>> fetchLanguages(){
        return remote.listLanguages("{}".getBytes())
                .flatMap(listLanguagesResponseConverter::read)
                .flatMapIterable(YandexListLanguagesResponse::getLanguages)
                .filter(yl->yl.getName() != null && yl.getCode() != null)
                .map(this::mapLanguage)
                .collectList();
    }

    private TranslateLanguage mapLanguage(YandexLanguage language) {
        return new TranslateLanguage(language.getCode(), capitalize(language.getName()),
                findCountryCode(language.getCode()));
    }

    private String capitalize(String str) {
        if(str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase()+str.substring(1);
    }

    private TranslatedText mapText(YandexTranslation translation) {
        return new TranslatedText(translation.getText(),
                languages.stream()
                    .filter(lang->lang.getCode().equalsIgnoreCase(translation.getDetectedLanguageCode()))
                    .findAny()
                    .orElseGet(()->new TranslateLanguage(translation.getDetectedLanguageCode(),
                            translation.getDetectedLanguageCode(), null)));
    }

    @Nullable
    private String findCountryCode(String langCode) {
        if(langCode == null) {
            return null;
        }
        switch(langCode) {
        case "en": return "GB";
        case "zh": return "CN";
        }
        List<Locale> localeCondidates = new ArrayList<>(Arrays.stream(Locale.getAvailableLocales())
                .filter(l->l.getLanguage().equalsIgnoreCase(langCode)
                        && l.getCountry() != null
                        && l.getCountry().length() == 2)
                .collect(Collectors.toList()));
        if(localeCondidates.isEmpty()) {
            return null;
        }
        localeCondidates.sort((l1, l2)->Integer.compare(dif(l1, langCode), dif(l2, langCode)));
        return localeCondidates.get(0).getCountry();
    }

    private int dif(Locale locale, String langCode) {
        String first = locale.getCountry().toLowerCase();
        String second = langCode.toLowerCase();
        if(first.length() != 2) {
            return 20;
        }
        int result = 10;
        for(int i = 0; i < 2 && i < second.length(); i++) {
            if(first.charAt(i) == second.charAt(i)) {
                result -= 2-i;
            }
        }
        return result;
    }

}
