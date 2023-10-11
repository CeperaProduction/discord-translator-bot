package me.cepera.discord.bot.translator.discord.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Message;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public interface TranslationProvider {

    TranslateService translateService();

    default Mono<TranslatedText> translate(Message message, TranslateLanguage targetLanguage){
        List<String> texts = new ArrayList<>();
        if(!message.getContent().trim().isEmpty()) {
            texts.add(message.getContent());
        }
        message.getEmbeds()
            .stream()
            .forEach(embed->{
                embed.getTitle().ifPresent(title->texts.add(title));
                embed.getDescription().ifPresent(desc->texts.add(desc));
                //embed.getFooter().ifPresent(footer->texts.add(footer.getText()));
            });

        return translate(texts, targetLanguage);
    }

    default Mono<TranslatedText> translate(List<String> texts, TranslateLanguage targetLanguage){

        if(texts.isEmpty()) {
            return Mono.empty();
        }

        return translateService().translate(texts, targetLanguage)
                .collectList()
                .filter(list->!list.isEmpty())
                .map(translations->{
                   TranslateLanguage sourceLanguage = translations.get(0).getOriginalLanguage();
                   List<String> translatedStrings = translations.stream().map(TranslatedText::getText).collect(Collectors.toList());
                   return new TranslatedText(String.join("\n\n", translatedStrings), sourceLanguage);
                });
    }

}
