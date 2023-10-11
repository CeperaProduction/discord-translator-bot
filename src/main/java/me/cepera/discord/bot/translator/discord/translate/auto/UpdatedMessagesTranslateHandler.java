package me.cepera.discord.bot.translator.discord.translate.auto;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Message;
import me.cepera.discord.bot.translator.discord.MessageOwnerResolver;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import me.cepera.discord.bot.translator.discord.translate.TranslationProvider;
import me.cepera.discord.bot.translator.discord.translate.TranslationSender;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class UpdatedMessagesTranslateHandler implements DiscordEventHandler<MessageUpdateEvent>, MessageOwnerResolver,
        TranslationProvider, TranslationSender{

    private final TranslateService translateService;

    private final AutoTranslatingChannelRepository autoTranslatingChannelRepository;

    private final DiscordClient discordClient;

    public UpdatedMessagesTranslateHandler(TranslateService translateService,
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
    public Class<MessageUpdateEvent> getEventClass() {
        return MessageUpdateEvent.class;
    }

    @Override
    public Mono<Void> handleEvent(MessageUpdateEvent event) {
        if(!event.isContentChanged() && !event.isEmbedsChanged()) {
            return Mono.empty();
        }
        return event.getMessage()
                .filterWhen(message->isNotMyMessage(message))
                .flatMap(message->message.getChannel()
                        .zipWhen(channel->autoTranslatingChannelRepository.getChannel(channel.getId().asLong())
                                .flatMap(auto->translateService.getLanguage(auto.getLanguageCode())))
                        .flatMap(tuple->tuple.getT1().getMessagesAfter(message.getId())
                                .take(50)
                                .filterWhen(reply->isMyMessage(reply))
                                .filter(reply->referencesTo(reply, message))
                                .filter(this::isAutoTranslationMessage)
                                .take(1)
                                .singleOrEmpty()
                                .map(reply->Tuples.of(reply, tuple.getT2())))
                        .flatMap(tuple->translate(message, tuple.getT2())
                                .flatMap(translated->sendTranslation(message, translated.getOriginalLanguage(), tuple.getT2(),
                                        translated.getText(), tuple.getT1()))))
                .then();
    }

    private boolean referencesTo(Message reply, Message referencedTo) {
        return reply.getMessageReference()
                .flatMap(ref->ref.getMessageId())
                .map(ref->ref.equals(referencedTo.getId()))
                .orElse(false);
    }

    private boolean isAutoTranslationMessage(Message translationMessage) {
        return !translationMessage.getEmbeds().isEmpty()
                && translationMessage.getEmbeds().get(0).getFooter()
                .map(footer->(footer.getText() == null || footer.getText().isEmpty())
                        && !footer.getIconUrl().isPresent())
                .orElse(true);
    }

}
