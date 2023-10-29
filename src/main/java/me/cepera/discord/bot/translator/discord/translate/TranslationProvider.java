package me.cepera.discord.bot.translator.discord.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Message;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public interface TranslationProvider {

    Pattern URL_PATTERN = Pattern.compile("(?<link>https?://[^\\s(){}\\[\\]]+)");
    Pattern URL_REPLACEMENT_PATTERN = Pattern.compile("#%\\$(?<number>\\d+)\\$%#");

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

        List<String> links = new ArrayList<String>();
        List<String> preparedTexts = new ArrayList<>(texts);

        int j = 0;
        for(int i = 0; i < preparedTexts.size(); ++i) {
            String text = preparedTexts.get(i);
            Matcher matcher = URL_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer();
            while(matcher.find()) {
                String link = matcher.group("link");
                matcher.appendReplacement(sb, "#%\\$"+j+"\\$%#");
                links.add(link);
                j++;
            }
            matcher.appendTail(sb);
            preparedTexts.set(i, sb.toString());
        }

        return translateService().translate(preparedTexts, targetLanguage)
                .collectList()
                .filter(list->!list.isEmpty())
                .map(translations->{
                   TranslateLanguage sourceLanguage = translations.get(0).getOriginalLanguage();
                   List<String> translatedStrings = new ArrayList<>(translations.stream()
                           .map(TranslatedText::getText)
                           .collect(Collectors.toList()));

                   for(int i = 0; i < translatedStrings.size(); ++i) {
                       String text = translatedStrings.get(i);
                       Matcher matcher = URL_REPLACEMENT_PATTERN.matcher(text);
                       StringBuffer sb = new StringBuffer();
                       while(matcher.find()) {
                           String link = links.get(Integer.parseInt(matcher.group("number")));
                           matcher.appendReplacement(sb, link.replace("$", "\\$"));
                       }
                       matcher.appendTail(sb);
                       translatedStrings.set(i, sb.toString());
                   }

                   return new TranslatedText(String.join("\n\n", translatedStrings), sourceLanguage);
                });
    }

}
