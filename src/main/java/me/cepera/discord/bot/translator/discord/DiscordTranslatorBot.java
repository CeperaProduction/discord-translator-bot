package me.cepera.discord.bot.translator.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.AllowedMentions;
import io.netty.util.internal.ThrowableUtil;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannel;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import me.cepera.discord.bot.translator.utils.UnicodeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class DiscordTranslatorBot extends BasicDiscordBot{

    private static final Logger LOGGER = LogManager.getLogger(DiscordTranslatorBot.class);

    public static final String COMMAND_SETUP = "setup";
    public static final String COMMAND_AUTO = "auto";

    public static final String MESSAGE_COMMAND_TRANSLATE = "Translate to";

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository translateLanguageUsageRepository;

    private final AutoTranslatingChannelRepository autoTranslatingChannelRepository;

    @Inject
    public DiscordTranslatorBot(DiscordBotConfig config, TranslateService translateService,
            TranslateLanguageUsageRepository translateLanguageUsageRepository,
            AutoTranslatingChannelRepository autoTranslatingChannelRepository) {
        super(config);
        this.translateService = translateService;
        this.translateLanguageUsageRepository = translateLanguageUsageRepository;
        this.autoTranslatingChannelRepository = autoTranslatingChannelRepository;
    }

    @Override
    public Flux<ApplicationCommandRequest> commandsToRegister() {
        return Flux.create(sink->{

            sink.next(ApplicationCommandRequest.builder()
                    .name(COMMAND_SETUP)
                    .description("Setting up or removing default languages")
                    .defaultMemberPermissions("8")
                    .dmPermission(false)
                    .build());

            sink.next(ApplicationCommandRequest.builder()
                    .name(COMMAND_AUTO)
                    .description("Start/stop automatic translation of messages in channel")
                    .defaultMemberPermissions("8")
                    .dmPermission(false)
                    .build());

            sink.next(ApplicationCommandRequest.builder()
                    .name(MESSAGE_COMMAND_TRANSLATE)
                    .dmPermission(false)
                    .type(3)
                    .build());

        });
    }

    @Override
    public void configureGatewayClient(GatewayDiscordClient client) {
        super.configureGatewayClient(client);

        client.on(SelectMenuInteractionEvent.class)
            .flatMap(event->this.handleSelectInteractionEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling selector interaction", e);
                        return Mono.empty();
                    }))
            .subscribe();

        client.on(MessageCreateEvent.class)
            .flatMap(event->this.handleMessageCreateEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling message create event", e);
                        return Mono.empty();
                    }))
            .subscribe();

        client.on(MessageUpdateEvent.class)
            .flatMap(event->this.handleMessageUpdateEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling message create event", e);
                        return Mono.empty();
                    }))
            .subscribe();

    }

    @Override
    protected Mono<Void> handleChatInputInteractionEvent(ChatInputInteractionEvent event) {
        if(event.getCommandName().equals(COMMAND_SETUP)) {
            return event.deferReply()
                    .withEphemeral(true)
                    .then(sendLanguateChooseMessage(event, "setup", Snowflake.of(1), 0))
                    .onErrorResume(e->sendErrorMessage(event, e));
        }
        if(event.getCommandName().equals(COMMAND_AUTO)) {
            return event.deferReply()
                    .withEphemeral(true)
                    .then(autoTranslatingChannelRepository.getChannel(event.getInteraction().getChannelId().asLong()))
                    .switchIfEmpty(Mono.defer(()->sendLanguateChooseMessage(event, "auto", Snowflake.of(1), 0)).then(Mono.empty()))
                    .flatMap(channel->autoTranslatingChannelRepository.removeChannel(channel.getChannelId())
                            .then(event.editReply()
                                    .withContentOrNull("Automatic translation for this channel was stopped")
                                    .then(Mono.fromRunnable(()->LOGGER.info("Autamatic translation for channel {} stopped by {}",
                                            channel.getChannelId(), event.getInteraction().getUser().getTag()))
                                            .then())))
                    .onErrorResume(e->sendErrorMessage(event, e));
        }
        return Mono.empty();
    }

    @Override
    protected Mono<Void> handleMessageInteractionEvent(MessageInteractionEvent event) {
        if(!event.getCommandName().startsWith(MESSAGE_COMMAND_TRANSLATE)) {
            return Mono.empty();
        }
        if(event.getCommandName().equals(MESSAGE_COMMAND_TRANSLATE)) {
            return event.deferReply()
                    .withEphemeral(true)
                    .then(event.getTargetMessage())
                    .flatMap(message->sendLanguateChooseMessage(event, "translate", message.getId(), 0));
        }

        String langName = event.getCommandName().substring(MESSAGE_COMMAND_TRANSLATE.length()+1);
        return event.deferReply()
                .then(translateService.getAvailableLanguages()
                    .filter(lang->lang.getName().equalsIgnoreCase(langName))
                    .take(1)
                    .singleOrEmpty())
                .zipWith(event.getTargetMessage())
                .flatMap(tuple->handleTranslationCommand(event, tuple.getT2(), tuple.getT1()))
                .then(event.deleteReply());
    }

    private Mono<Void> handleSelectInteractionEvent(SelectMenuInteractionEvent event){
        String prefix = "language-";
        if(!event.getCustomId().startsWith(prefix)) {
            return Mono.empty();
        }
        if(event.getValues().isEmpty()) {
            return Mono.empty();
        }
        int lastSplit = event.getCustomId().lastIndexOf('-');
        String operation = event.getCustomId().substring(prefix.length(), lastSplit);
        long id = Long.parseLong(event.getCustomId().substring(lastSplit+1));
        if(id == 0) {
            return Mono.empty();
        }
        String selectedValue = event.getValues().get(0);
        String paginationPrefix = "next-";
        if(selectedValue.startsWith(paginationPrefix)) {
            return event.deferEdit()
                    .then(sendLanguateChooseMessage(event, operation, Snowflake.of(id),
                            Integer.parseInt(selectedValue.substring(paginationPrefix.length()))));
        }

        switch(operation) {
        case "translate":
            return event.deferEdit()
                    .then(event.getInteraction().getChannel())
                    .flatMap(channel->channel.getMessageById(Snowflake.of(id)))
                    .flatMap(message->translateService.getLanguage(selectedValue)
                            .flatMap(lang->handleTranslationCommand(event, message, lang)))
                    .then(event.deleteReply());
        case "setup":
            return event.deferEdit()
                    .withEphemeral(true)
                    .then(translateService.getLanguage(selectedValue))
                    .flatMap(language->setupLanguageCommand(event, language));
        case "auto":
            return event.deferEdit()
                    .withEphemeral(true)
                    .then(translateService.getLanguage(selectedValue))
                    .flatMap(language->setupAutoTranslation(event, language));
        default: return Mono.empty();
        }
    }

    private Mono<Void> handleMessageCreateEvent(MessageCreateEvent event){
        return isMyMessage(event.getMessage())
                .filter(my->!my)
                .flatMap(my->event.getMessage().getChannel()
                    .zipWith(autoTranslatingChannelRepository.getChannel(event.getMessage().getChannelId().asLong())
                            .flatMap(auto->translateService.getLanguage(auto.getLanguageCode())))
                    .flatMap(tuple->translate(event.getMessage(), tuple.getT2())
                            .flatMap(translated->sendTranslation(event.getMessage(),
                                    translated.getOriginalLanguage(), tuple.getT2(), translated.getText())))
                    .then());
    }

    private Mono<Void> handleMessageUpdateEvent(MessageUpdateEvent event){
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
                                .filter(reply->reply.getMessageReference()
                                        .flatMap(ref->ref.getMessageId())
                                        .map(ref->ref.equals(message.getId()))
                                        .orElse(false))
                                .filter(reply->!reply.getEmbeds().isEmpty()
                                        && reply.getEmbeds().get(0).getFooter()
                                            .map(footer->(footer.getText() == null || footer.getText().isEmpty())
                                                    && !footer.getIconUrl().isPresent())
                                            .orElse(true))
                                .take(1)
                                .singleOrEmpty()
                                .map(reply->Tuples.of(reply, tuple.getT2())))
                        .flatMap(tuple->translate(message, tuple.getT2())
                                .flatMap(translated->sendTranslation(message, translated.getOriginalLanguage(), tuple.getT2(),
                                        translated.getText(), tuple.getT1()))))
                .then();
    }

    private Mono<Boolean> isMyMessage(Message message){
        return getDiscordClient().getSelf().zipWith(Mono.justOrEmpty(message.getAuthor()))
            .filter(tuple->tuple.getT1().id().asLong() == tuple.getT2().getId().asLong())
            .map(tuple->Boolean.TRUE)
            .switchIfEmpty(Mono.just(Boolean.FALSE));
    }

    private Mono<Boolean> isNotMyMessage(Message message){
        return isMyMessage(message).map(res->!res);
    }

    private Mono<Void> sendLanguateChooseMessage(DeferrableInteractionEvent event, String operation, Snowflake messageId, int startLangIndex){
        int maxLanguagesOnPage = 15;
        return translateLanguageUsageRepository.getLastLanguageCode(event.getInteraction().getUser().getId().asLong())
                .map(lang->Arrays.asList(lang))
                .flatMapMany(first->translateService.getAvailableLanguages(first))
                .switchIfEmpty(Flux.defer(()->translateService.getAvailableLanguages()))
                .flatMap(lang->unicodeFlagEmoji(lang)
                        .map(emoji->Option.of(lang.getName(), lang.getCode()).withEmoji(ReactionEmoji.unicode(emoji)))
                        .switchIfEmpty(Mono.fromSupplier(()->Option.of(lang.getName(), lang.getCode()))))
                .skip(startLangIndex)
                .take(maxLanguagesOnPage)
                .collectList()
                .flatMap(languageOptions->{

                    int nextIndex = startLangIndex + languageOptions.size();

                    List<LayoutComponent> components = new ArrayList<>();

                    Set<String> added = new HashSet<>();
                    List<Option> finalLanguageOptions = new ArrayList<>();
                    languageOptions.stream().filter(opt->added.add(opt.getValue()))
                        .forEach(finalLanguageOptions::add);
                    if(startLangIndex != 0) {
                        finalLanguageOptions.add(0, Option.of("Previous...", "next-"+Math.max(0, startLangIndex-maxLanguagesOnPage)));
                    }
                    if(languageOptions.size() == maxLanguagesOnPage) {
                        finalLanguageOptions.add(Option.of("Next...", "next-"+nextIndex));
                    }
                    SelectMenu selectLanguage = SelectMenu.of("language-"+operation+"-" + messageId.asLong(),
                            finalLanguageOptions)
                            .withMaxValues(1)
                            .withMinValues(1)
                            .withPlaceholder("Select target language...");

                    components.add(ActionRow.of(selectLanguage));

                    return event.editReply()
                        .withComponentsOrNull(components)
                        .then();
                })
                .onErrorResume(e->sendErrorMessage(event, e));
    }

    private Mono<Void> handleTranslationCommand(DeferrableInteractionEvent event, Message message, TranslateLanguage targetLanguage){
        return translate(message, targetLanguage)
                .flatMap(text->sendTranslation(event, message, text.getOriginalLanguage(), targetLanguage, text.getText()))
                .onErrorResume(e->sendErrorMessage(event, e));
    }

    private Mono<TranslatedText> translate(Message message, TranslateLanguage targetLanguage){
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

        if(texts.isEmpty()) {
            return Mono.empty();
        }

        return translateService.translate(texts, targetLanguage)
                .collectList()
                .filter(list->!list.isEmpty())
                .map(translations->{
                   TranslateLanguage sourceLanguage = translations.get(0).getOriginalLanguage();
                   List<String> translatedStrings = translations.stream().map(TranslatedText::getText).collect(Collectors.toList());
                   return new TranslatedText(String.join("\n\n", translatedStrings), sourceLanguage);
                });
    }

    private Mono<Void> setupLanguageCommand(DeferrableInteractionEvent event, TranslateLanguage language){
        String commandName = MESSAGE_COMMAND_TRANSLATE + " " + language.getName();
        return event.getInteraction().getGuild()
                .flatMap(guild->getDiscordClient().getApplicationId()
                        .flatMap(appId->getDiscordClient().getApplicationService()
                                .getGuildApplicationCommands(appId, guild.getId().asLong())
                                .filter(cmd->cmd.name().equals(commandName))
                                .take(1)
                                .singleOrEmpty()
                                .flatMap(cmd->getDiscordClient().getApplicationService()
                                        .deleteGuildApplicationCommand(appId, guild.getId().asLong(), cmd.id().asLong())
                                        .then(Mono.fromRunnable(()->LOGGER.info("{} removed default lang {} from server {}",
                                                event.getInteraction().getUser().getTag(),
                                                language.getCode(),
                                                guild.getId().asLong())))
                                        .then(event.editReply()
                                                .withContentOrNull(commandName+" default command removed.")
                                                .withComponentsOrNull(Collections.emptyList()))
                                        .then())
                                .switchIfEmpty(Mono.defer(()->getDiscordClient().getApplicationService()
                                        .createGuildApplicationCommand(appId, guild.getId().asLong(), ApplicationCommandRequest.builder()
                                                .name(commandName)
                                                .type(3)
                                                .build())
                                        .doOnNext(cmd->LOGGER.info("{} added default lang {} to server {}",
                                                event.getInteraction().getUser().getTag(),
                                                language.getCode(),
                                                guild.getId().asLong()))
                                        .then(event.editReply()
                                                .withContentOrNull(commandName+" default command added.")
                                                .withComponentsOrNull(Collections.emptyList()))
                                        .then()))));
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

    private Mono<Void> sendTranslation(Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent){
        return sendTranslation(message, Optional.empty(), Optional.empty(), sourceLanguage, targetLanguage, translatedContent);
    }

    private Mono<Void> sendTranslation(Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent, Message messageToEdit){
        return sendTranslation(message, Optional.empty(), Optional.empty(), sourceLanguage, targetLanguage, translatedContent, messageToEdit);
    }

    private Mono<Void> sendTranslation(DeferrableInteractionEvent event, Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent){
        return sendTranslation(message, event.getInteraction().getMember(), Optional.of(event.getInteraction().getUser()),
                        sourceLanguage, targetLanguage, translatedContent)
                .then(translateLanguageUsageRepository.setLastLanguageCode(event.getInteraction().getUser().getId().asLong(),
                        targetLanguage.getCode())
                        .onErrorResume(e->Mono.fromRunnable(()->
                            LOGGER.error("Error on saving last language: {}", ThrowableUtil.stackTraceToString(e)))));
    }

    private Mono<Void> sendTranslation(Message message, Optional<Member> optMember, Optional<User> optUser,
            TranslateLanguage sourceLanguage, TranslateLanguage targetLanguage, String translatedContent){
        return getFlags(sourceLanguage, targetLanguage)
                .flatMap(langEmojiTuple->message.getChannel()
                        .flatMap(channel->channel.createMessage(MessageCreateSpec.builder()
                                .messageReference(message.getId())
                                .allowedMentions(AllowedMentions.suppressAll())
                                .addEmbed(EmbedCreateSpec.builder()
                                    .description(translatedContent)
                                    .addField("From", langEmojiTuple.getT1()+sourceLanguage.getName(), true)
                                    .addField("To", langEmojiTuple.getT2()+targetLanguage.getName(), true)
                                    .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                    .footer(getCommandSenderName(optMember, optUser), optUser.map(user->user.getAvatarUrl()).orElse(null))
                                    .build())
                                .build())))
                .then();
    }

    private Mono<Void> sendTranslation(Message message, Optional<Member> optMember, Optional<User> optUser,
            TranslateLanguage sourceLanguage, TranslateLanguage targetLanguage, String translatedContent, Message messageToEdit){
        return getFlags(sourceLanguage, targetLanguage)
                .flatMap(langEmojiTuple->messageToEdit.edit(MessageEditSpec.builder()
                        .allowedMentionsOrNull(AllowedMentions.suppressAll())
                        .embedsOrNull(Arrays.asList(EmbedCreateSpec.builder()
                            .description(translatedContent)
                            .addField("From", langEmojiTuple.getT1()+sourceLanguage.getName(), true)
                            .addField("To", langEmojiTuple.getT2()+targetLanguage.getName(), true)
                            .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                            .footer(getCommandSenderName(optMember, optUser), optUser.map(user->user.getAvatarUrl()).orElse(null))
                            .build()))
                        .build()))
                .then();
    }

    private Mono<Tuple2<String, String>> getFlags(TranslateLanguage lang1, TranslateLanguage lang2){
        return Mono.zip(unicodeFlagEmoji(lang1)
                .map(emoji->emoji+" ")
                .switchIfEmpty(Mono.just("")),
            unicodeFlagEmoji(lang2)
                .map(emoji->emoji+" ")
                .switchIfEmpty(Mono.just("")));
    }

    private Mono<Void> sendErrorMessage(DeferrableInteractionEvent event, Throwable e){
        LOGGER.error("An unexpected error has occurred while processing interaction. {}",
                ThrowableUtil.stackTraceToString(e));
        String message = "An unexpected error has occurred. "+e.toString();
        if(message.length() > 1980) {
            message = message.substring(0, 1980)+"...";
        }
        return event.editReply()
            .withContentOrNull(message)
            .then();
    }

    private String getCommandSenderName(Optional<Member> optMember, Optional<User> optUser) {
        return optMember.map(member->member.getDisplayName())
                .orElseGet(()->optUser.flatMap(User::getGlobalName).orElse(""));
    }

    private Mono<String> unicodeFlagEmoji(TranslateLanguage lang){
        if(lang.getCountryCode() == null) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(UnicodeUtils.unicodeFlagEmoji(lang.getCountryCode()));
    }

}
