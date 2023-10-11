package me.cepera.discord.bot.translator.discord.translate;

import java.util.Arrays;
import java.util.List;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.reaction.ReactionEmoji;
import io.netty.util.internal.ThrowableUtil;
import me.cepera.discord.bot.translator.discord.DiscordErrorHandler;
import me.cepera.discord.bot.translator.discord.PagedSelectProvider;
import me.cepera.discord.bot.translator.discord.UnicodeEmojiProvider;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LanguageSelectProvider extends PagedSelectProvider, DiscordErrorHandler, UnicodeEmojiProvider{

    TranslateService translateService();

    TranslateLanguageUsageRepository usageRepository();

    default Mono<Void> sendLanguateChooseMessage(DeferrableInteractionEvent event, int startLangIndex, List<String> operands){
        Flux<Option> optionFlux = usageRepository().getLastLanguageCode(event.getInteraction().getUser().getId().asLong())
                .onErrorResume(e->Mono.fromRunnable(()->logger().error("Failed to get last used language for user {}: {}",
                        event.getInteraction().getUser().getTag(),
                        ThrowableUtil.stackTraceToString(e))))
                .map(lang->Arrays.asList(lang))
                .flatMapMany(first->translateService().getAvailableLanguages(first))
                .switchIfEmpty(Flux.defer(()->translateService().getAvailableLanguages()))
                .flatMap(lang->unicodeFlagEmoji(lang)
                        .map(emoji->Option.of(lang.getName(), lang.getCode()).withEmoji(ReactionEmoji.unicode(emoji)))
                        .switchIfEmpty(Mono.fromSupplier(()->Option.of(lang.getName(), lang.getCode()))));

        return getSelector(optionFlux, "Select target language...", startLangIndex, operands)
                .flatMap(select->event.editReply()
                        .withComponentsOrNull(Arrays.asList(ActionRow.of(select)))
                        .then())
                .onErrorResume(e->sendErrorMessage(event, e));
    }

}
