package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import me.cepera.discord.bot.translator.discord.translate.DiscordTranslatorBot;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectProvider;
import me.cepera.discord.bot.translator.discord.translate.TranslationProvider;
import me.cepera.discord.bot.translator.discord.translate.TranslationSender;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class MessageInteractionTranslateHandler implements DiscordEventHandler<MessageInteractionEvent>,
        TranslationProvider, TranslationSender, LanguageSelectProvider{

    private static final Logger LOGGER = LogManager.getLogger(MessageInteractionTranslateHandler.class);

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository languageUsageRepository;

    private final TranslateLanguageSelectOperationHandler translateSelectHandler;

    public MessageInteractionTranslateHandler(TranslateService translateService,
            TranslateLanguageUsageRepository languageUsageRepository,
            TranslateLanguageSelectOperationHandler translateSelectHandler) {
        this.translateService = translateService;
        this.languageUsageRepository = languageUsageRepository;
        this.translateSelectHandler = translateSelectHandler;
    }

    @Override
    public Class<MessageInteractionEvent> getEventClass() {
        return MessageInteractionEvent.class;
    }

    @Override
    public TranslateService translateService() {
        return translateService;
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public TranslateLanguageUsageRepository usageRepository() {
        return languageUsageRepository;
    }

    @Override
    public Mono<Void> handleEvent(MessageInteractionEvent event) {
        if(!event.getCommandName().startsWith(DiscordTranslatorBot.MESSAGE_COMMAND_TRANSLATE)) {
            return Mono.empty();
        }

        if(event.getCommandName().equals(DiscordTranslatorBot.MESSAGE_COMMAND_TRANSLATE)) {
            return event.deferReply()
                    .withEphemeral(true)
                    .then(event.getTargetMessage())
                    .flatMap(message->sendLanguateChooseMessage(event, 0, Arrays.asList(Long.toString(message.getId().asLong()))));
        }

        String langName = event.getCommandName().substring(DiscordTranslatorBot.MESSAGE_COMMAND_TRANSLATE.length()+1);

        return event.deferReply()
                .then(getLanguageByName(langName))
                .zipWith(event.getTargetMessage())
                .flatMap(tuple->translate(tuple.getT2(), tuple.getT1())
                        .flatMap(translation->sendTranslation(event, tuple.getT2(),
                                translation.getOriginalLanguage(), tuple.getT1(), translation.getText())))
                .then(event.deleteReply());
    }

    private Mono<TranslateLanguage> getLanguageByName(String langName){
        return translateService.getAvailableLanguages()
                .filter(lang->lang.getName().equalsIgnoreCase(langName))
                .take(1)
                .singleOrEmpty();
    }

    @Override
    public String getSelectorId(List<String> operands) {
        return translateSelectHandler.getSelectorId(operands);
    }

}
