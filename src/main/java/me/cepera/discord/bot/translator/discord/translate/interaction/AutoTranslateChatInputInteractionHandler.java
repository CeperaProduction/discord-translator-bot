package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import me.cepera.discord.bot.translator.discord.translate.DiscordTranslatorBot;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectProvider;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class AutoTranslateChatInputInteractionHandler implements DiscordEventHandler<ChatInputInteractionEvent>, LanguageSelectProvider{

    private static final Logger LOGGER = LogManager.getLogger(AutoTranslateChatInputInteractionHandler.class);

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository translateLanguageUsageRepository;

    private final AutoTranslatingChannelRepository autoTranslatingChannelRepository;

    private final AutoTranslateSelectLanguageOperationHandler autoTranslateSelectHandler;

    public AutoTranslateChatInputInteractionHandler(TranslateService translateService,
            TranslateLanguageUsageRepository translateLanguageUsageRepository,
            AutoTranslatingChannelRepository autoTranslatingChannelRepository,
            AutoTranslateSelectLanguageOperationHandler autoTranslateSelectHandler) {
        this.translateService = translateService;
        this.translateLanguageUsageRepository = translateLanguageUsageRepository;
        this.autoTranslatingChannelRepository = autoTranslatingChannelRepository;
        this.autoTranslateSelectHandler = autoTranslateSelectHandler;
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventClass() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public TranslateService translateService() {
        return translateService;
    }

    @Override
    public TranslateLanguageUsageRepository usageRepository() {
        return translateLanguageUsageRepository;
    }

    @Override
    public String getSelectorId(List<String> operands) {
        return autoTranslateSelectHandler.getSelectorId(operands);
    }

    @Override
    public Mono<Void> handleEvent(ChatInputInteractionEvent event) {
        if(!event.getCommandName().equals(DiscordTranslatorBot.COMMAND_AUTO)) {
            return Mono.empty();
        }
        return event.deferReply()
                .withEphemeral(true)
                .then(autoTranslatingChannelRepository.getChannel(event.getInteraction().getChannelId().asLong()))
                .switchIfEmpty(Mono.defer(()->sendLanguateChooseMessage(event, 0, Collections.emptyList())).then(Mono.empty()))
                .flatMap(channel->autoTranslatingChannelRepository.removeChannel(channel.getChannelId())
                        .then(event.editReply()
                                .withContentOrNull("Automatic translation for this channel was stopped")
                                .then(Mono.fromRunnable(()->LOGGER.info("Autamatic translation for channel {} stopped by {}",
                                        channel.getChannelId(), event.getInteraction().getUser().getTag()))
                                        .then())))
                .onErrorResume(e->sendErrorMessage(event, e));
    }

}
