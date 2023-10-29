package me.cepera.discord.bot.translator.discord.translate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TranslationProviderTest implements TranslationProvider{

    private static final Logger LOGGER = LogManager.getLogger(TranslationProviderTest.class);

    TranslateService translateService = new TranslateService() {

        @Override
        public Flux<TranslatedText> translate(List<String> texts, TranslateLanguage targetLanguage,
                Optional<TranslateLanguage> sourceLanguage) {
            LOGGER.info("Received for translation: {}", texts);
            return Flux.fromIterable(texts)
                    .map(text->new TranslatedText(text, targetLanguage));
        }

        @Override
        public Mono<TranslateLanguage> getLanguage(String code) {
            return Mono.just(new TranslateLanguage(code, code, code));
        }

        @Override
        public Flux<TranslateLanguage> getAvailableLanguages(List<String> firstLanguageIds) {
            return Flux.empty();
        }

        @Override
        public Flux<TranslateLanguage> getAvailableLanguages() {
            return Flux.empty();
        }
    };

    @Override
    public TranslateService translateService() {
        return translateService;
    }

    @Test
    void testLinksProcessing() {


        List<String> texts = new ArrayList<>();

        texts.add("Text without any links");
        texts.add("[A link](https://yandex.ru/1)");
        texts.add("Some text with a single [link](https://yandex.ru/2) in content");
        texts.add("Some text with a multiple [lin$ks$](https://yandex.ru/3?a=4&$abc=1) in content. Lets [see](https://yandex.ru/4$) what will happen.");
        texts.add("http://a.link/someLink");

        LOGGER.info("Original texts: {}", texts);

        TranslatedText translated = translateService().getLanguage("ru")
                .flatMap(lang->translate(texts, lang))
                .block();

        LOGGER.info("Translated texts: {}", translated.getText());

        assertEquals(String.join("\n\n", texts), translated.getText(), "Received wrong text");

    }


}
