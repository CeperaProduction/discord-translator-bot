package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.List;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectMenuEventHandler;
import me.cepera.discord.bot.translator.discord.translate.TranslationProvider;
import me.cepera.discord.bot.translator.discord.translate.TranslationSender;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class TranslateLanguageSelectOperationHandler extends LanguageSelectMenuEventHandler implements TranslationProvider, TranslationSender{

    public static final String OPERATION_TRANSLATE = "translate";

    public TranslateLanguageSelectOperationHandler(TranslateService translateService, TranslateLanguageUsageRepository usageRepository) {
        super(translateService, usageRepository, OPERATION_TRANSLATE);
    }

    @Override
    protected Mono<Void> handleLanguageSelection(SelectMenuInteractionEvent event, TranslateLanguage language,
            List<String> operands) {
        if(operands.isEmpty()) {
            return Mono.empty();
        }
        long messageId = Long.parseLong(operands.get(0));
        return event.deferEdit()
                .then(event.getInteraction().getChannel())
                .flatMap(channel->channel.getMessageById(Snowflake.of(messageId)))
                .flatMap(message->translate(message, language)
                        .flatMap(translation->sendTranslation(event, message,
                                translation.getOriginalLanguage(), language, translation.getText())))
                .then(event.deleteReply());
    }

}
