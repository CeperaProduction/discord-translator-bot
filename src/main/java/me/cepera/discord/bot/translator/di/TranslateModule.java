package me.cepera.discord.bot.translator.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.cepera.discord.bot.translator.converter.BodyConverter;
import me.cepera.discord.bot.translator.converter.GenericJsonBodyConverter;
import me.cepera.discord.bot.translator.dto.yandex.YandexListLanguagesResponse;
import me.cepera.discord.bot.translator.dto.yandex.YandexTranslateRequest;
import me.cepera.discord.bot.translator.dto.yandex.YandexTranslateResponse;
import me.cepera.discord.bot.translator.service.TranslateService;
import me.cepera.discord.bot.translator.service.impl.YandexTranslateService;

@Module
public class TranslateModule {

    @Provides
    @Singleton
    TranslateService translateService(YandexTranslateService translateService) {
        return translateService;
    }

    @Provides
    BodyConverter<YandexTranslateRequest> translateRequestBodyConverter(){
        return new GenericJsonBodyConverter<>(YandexTranslateRequest.class);
    }

    @Provides
    BodyConverter<YandexTranslateResponse> translateResponseBodyConverter(){
        return new GenericJsonBodyConverter<>(YandexTranslateResponse.class);
    }

    @Provides
    BodyConverter<YandexListLanguagesResponse> listLanguagesResponseBodyConverter(){
        return new GenericJsonBodyConverter<>(YandexListLanguagesResponse.class);
    }

}
