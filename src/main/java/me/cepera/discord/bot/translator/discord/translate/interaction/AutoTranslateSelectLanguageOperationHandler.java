package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectMenuEventHandler;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannel;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class AutoTranslateSelectLanguageOperationHandler extends LanguageSelectMenuEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(AutoTranslateSelectLanguageOperationHandler.class);

    public static final String OPERATION_AUTO_TRANSLATE = "auto";

    private final AutoTranslatingChannelRepository autoTranslatingChannelRepository;

    public AutoTranslateSelectLanguageOperationHandler(TranslateService translateService,
            TranslateLanguageUsageRepository usageRepository,
            AutoTranslatingChannelRepository autoTranslatingChannelRepository) {
        super(translateService, usageRepository, OPERATION_AUTO_TRANSLATE);
        this.autoTranslatingChannelRepository = autoTranslatingChannelRepository;
    }

    @Override
    protected Mono<Void> handleLanguageSelection(SelectMenuInteractionEvent event, TranslateLanguage language,
            List<String> operands) {
        return event.deferEdit()
                .withEphemeral(true)
                .then(setupAutoTranslation(event, language));
    }

    private Mono<Void> setupAutoTranslation(DeferrableInteractionEvent event, TranslateLanguage language){
        return autoTranslatingChannelRepository.setChannel(new AutoTranslatingChannel(
                    event.getInteraction().getChannelId().asLong(), language.getCode()))
                .then(event.editReply()
                        .withContentOrNull("Automatic translation for this channel was started.")
                        .withComponentsOrNull(Collections.emptyList())
                        .then(Mono.fromRunnable(()->LOGGER.info("Autamatic translation for channel {} started by {}",
                                event.getInteraction().getChannelId(), event.getInteraction().getUser().getTag())))
                        .then());
    }

}
