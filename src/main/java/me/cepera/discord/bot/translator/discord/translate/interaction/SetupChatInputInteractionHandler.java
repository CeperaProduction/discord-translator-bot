package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import me.cepera.discord.bot.translator.discord.translate.DiscordTranslatorBot;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectProvider;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class SetupChatInputInteractionHandler implements DiscordEventHandler<ChatInputInteractionEvent>, LanguageSelectProvider{

    private static final Logger LOGGER = LogManager.getLogger(SetupChatInputInteractionHandler.class);

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository translateLanguageUsageRepository;

    private final SetupLanguageSelectOperationHandler setupSelectHandler;

    public SetupChatInputInteractionHandler(TranslateService translateService,
            TranslateLanguageUsageRepository translateLanguageUsageRepository,
            SetupLanguageSelectOperationHandler setupSelectHandler) {
        this.translateService = translateService;
        this.translateLanguageUsageRepository = translateLanguageUsageRepository;
        this.setupSelectHandler = setupSelectHandler;
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
        return setupSelectHandler.getSelectorId(operands);
    }

    @Override
    public Mono<Void> handleEvent(ChatInputInteractionEvent event) {
        if(!event.getCommandName().equals(DiscordTranslatorBot.COMMAND_SETUP)) {
            return Mono.empty();
        }
        return event.deferReply()
                .withEphemeral(true)
                .then(sendLanguateChooseMessage(event, 0, Collections.emptyList()));
    }

}
