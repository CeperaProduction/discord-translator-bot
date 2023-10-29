package me.cepera.discord.bot.translator.discord.translate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import me.cepera.discord.bot.translator.discord.UnicodeEmojiProvider;
import me.cepera.discord.bot.translator.discord.UserNameResolver;
import me.cepera.discord.bot.translator.discord.translate.interaction.HideTranslationButtonInteractionHandler;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface TranslationSender extends UnicodeEmojiProvider, UserNameResolver{

    default Mono<Void> sendTranslation(Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent, boolean cleanButton){
        return sendTranslation(message, Optional.empty(), Optional.empty(), sourceLanguage, targetLanguage, translatedContent, cleanButton);
    }

    default Mono<Void> sendTranslation(Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent, Message messageToEdit){
        return sendTranslation(message, Optional.empty(), Optional.empty(), sourceLanguage, targetLanguage, translatedContent, messageToEdit);
    }

    default Mono<Void> sendTranslation(DeferrableInteractionEvent event, Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent, boolean cleanButton){
        return sendTranslation(message, event.getInteraction().getMember(), Optional.of(event.getInteraction().getUser()),
                        sourceLanguage, targetLanguage, translatedContent, cleanButton);
    }

    default Mono<Void> sendTranslation(Message message, Optional<Member> optMember, Optional<User> optUser,
            TranslateLanguage sourceLanguage, TranslateLanguage targetLanguage, String translatedContent, boolean cleanButton){
        return getFlagsPrefix(sourceLanguage, targetLanguage)
                .flatMap(langEmojiTuple->message.getChannel()
                        .flatMap(channel->channel.createMessage(MessageCreateSpec.builder()
                                .messageReference(message.getId())
                                .allowedMentions(AllowedMentions.suppressAll())
                                .addEmbed(EmbedCreateSpec.builder()
                                    .description(translatedContent)
                                    .addField("From", langEmojiTuple.getT1()+sourceLanguage.getName(), true)
                                    .addField("To", langEmojiTuple.getT2()+targetLanguage.getName(), true)
                                    .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                    .footer(getUserName(optMember, optUser), optUser.map(user->user.getAvatarUrl()).orElse(null))
                                    .build())
                                .components(cleanButton(cleanButton))
                                .build())))
                .then();
    }

    default Mono<Void> sendTranslation(Message message, Optional<Member> optMember, Optional<User> optUser,
            TranslateLanguage sourceLanguage, TranslateLanguage targetLanguage, String translatedContent, Message messageToEdit){
        return getFlagsPrefix(sourceLanguage, targetLanguage)
                .flatMap(langEmojiTuple->messageToEdit.edit(MessageEditSpec.builder()
                        .allowedMentionsOrNull(AllowedMentions.suppressAll())
                        .embedsOrNull(Arrays.asList(EmbedCreateSpec.builder()
                            .description(translatedContent)
                            .addField("From", langEmojiTuple.getT1()+sourceLanguage.getName(), true)
                            .addField("To", langEmojiTuple.getT2()+targetLanguage.getName(), true)
                            .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                            .footer(getUserName(optMember, optUser), optUser.map(user->user.getAvatarUrl()).orElse(null))
                            .build()))
                        .build()))
                .then();
    }

    default Mono<Tuple2<String, String>> getFlagsPrefix(TranslateLanguage lang1, TranslateLanguage lang2){
        return Mono.zip(unicodeFlagEmoji(lang1)
                .map(emoji->emoji+" ")
                .switchIfEmpty(Mono.just("")),
            unicodeFlagEmoji(lang2)
                .map(emoji->emoji+" ")
                .switchIfEmpty(Mono.just("")));
    }

    default Possible<List<LayoutComponent>> cleanButton(boolean needToAdd){
        if(!needToAdd) {
            return Possible.absent();
        }
        List<LayoutComponent> components = new ArrayList<>();
        components.add(ActionRow.of(Button.secondary(
                HideTranslationButtonInteractionHandler.BUTTON_ID, "X")));
        return Possible.of(components);
    }

}
