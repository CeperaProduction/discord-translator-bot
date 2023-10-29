package me.cepera.discord.bot.translator.discord.translate.auto;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.cepera.discord.bot.translator.discord.MessageOwnerResolver;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import me.cepera.discord.bot.translator.discord.translate.TranslationProvider;
import me.cepera.discord.bot.translator.discord.translate.TranslationSender;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class CreatedMessagesTranslateHandler implements DiscordEventHandler<MessageCreateEvent>, MessageOwnerResolver,
        TranslationProvider, TranslationSender{

    private final TranslateService translateService;

    private final AutoTranslatingChannelRepository autoTranslatingChannelRepository;

    private final DiscordClient discordClient;

    public CreatedMessagesTranslateHandler(TranslateService translateService,
            AutoTranslatingChannelRepository autoTranslatingChannelRepository, DiscordClient discordClient) {
        this.translateService = translateService;
        this.autoTranslatingChannelRepository = autoTranslatingChannelRepository;
        this.discordClient = discordClient;
    }

    @Override
    public TranslateService translateService() {
        return translateService;
    }

    @Override
    public DiscordClient discordClient() {
        return discordClient;
    }

    @Override
    public Class<MessageCreateEvent> getEventClass() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> handleEvent(MessageCreateEvent event) {
        return notMyMessageOrEmpty(event.getMessage())
                .flatMap(my->event.getMessage().getChannel()
                    .zipWith(autoTranslatingChannelRepository.getChannel(event.getMessage().getChannelId().asLong())
                            .flatMap(auto->translateService.getLanguage(auto.getLanguageCode())))
                    .flatMap(tuple->translate(event.getMessage(), tuple.getT2())
                            .flatMap(translated->sendTranslation(event.getMessage(),
                                    translated.getOriginalLanguage(), tuple.getT2(), translated.getText(), false)))
                    .then());
    }

}
