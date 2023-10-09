package me.cepera.discord.bot.translator.service.remote;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import me.cepera.discord.bot.translator.config.TranslatorConfig;
import reactor.core.publisher.Mono;

public class YandexTranslatorRemoteService implements RemoteService{

    private final TranslatorConfig translatorConfig;

    private final String uriBase = "https://translate.api.cloud.yandex.net/translate/v2";

    @Inject
    public YandexTranslatorRemoteService(TranslatorConfig translatorConfig) {
        this.translatorConfig = translatorConfig;
    }

    public Mono<byte[]> translate(byte[] requestBody){
        URI translateUri = URI.create(uriBase + "/translate");
        return post(translateUri, authHeaders(), requestBody);
    }
    
    public Mono<byte[]> listLanguages(byte[] requestBody){
        URI translateUri = URI.create(uriBase + "/languages");
        return post(translateUri, authHeaders(), requestBody);
    }

    private Map<String, String> authHeaders(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Api-Key "+translatorConfig.getApiKey());
        return headers;
    }

}
