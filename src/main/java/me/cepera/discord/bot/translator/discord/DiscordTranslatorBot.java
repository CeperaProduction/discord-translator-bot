package me.cepera.discord.bot.translator.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.AllowedMentions;
import io.netty.util.internal.ThrowableUtil;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.model.TranslatedText;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import me.cepera.discord.bot.translator.utils.UnicodeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class DiscordTranslatorBot extends BasicDiscordBot{

    private static final Logger LOGGER = LogManager.getLogger(DiscordTranslatorBot.class);

    public static final String COMMAND_SETUP = "setup";

    public static final String MESSAGE_COMMAND_TRANSLATE = "Translate to";

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository translateLanguageUsageRepository;

    @Inject
    public DiscordTranslatorBot(DiscordBotConfig config, TranslateService translateService,
            TranslateLanguageUsageRepository translateLanguageUsageRepository) {
        super(config);
        this.translateService = translateService;
        this.translateLanguageUsageRepository = translateLanguageUsageRepository;
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

    }

    @Override
    protected Mono<Void> handleChatInputInteractionEvent(ChatInputInteractionEvent event) {
        if(!event.getCommandName().equals(COMMAND_SETUP)) {
            return Mono.empty();
        }
        return event.deferReply()
                .withEphemeral(true)
                .then(sendLanguateChooseMessage(event, "setup", Snowflake.of(1), 0))
                .onErrorResume(e->sendErrorMessage(event, e));
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
                    .then(translateService.getLanguage(selectedValue))
                    .flatMap(language->setupLanguageCommand(event, language));
        default: return Mono.empty();
        }
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

                    List<Option> finalLanguageOptions = new ArrayList<>(languageOptions);
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
                   return Tuples.of(String.join("\n\n", translatedStrings), sourceLanguage);
                })
                .flatMap(tuple->sendTranslation(event, message, tuple.getT2(), targetLanguage, tuple.getT1()))
                .onErrorResume(e->sendErrorMessage(event, e));
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

    private Mono<Void> sendTranslation(DeferrableInteractionEvent event, Message message, TranslateLanguage sourceLanguage,
            TranslateLanguage targetLanguage, String translatedContent){
        return Mono.zip(unicodeFlagEmoji(sourceLanguage)
                        .map(emoji->emoji+" ")
                        .switchIfEmpty(Mono.just("")),
                    unicodeFlagEmoji(targetLanguage)
                        .map(emoji->emoji+" ")
                        .switchIfEmpty(Mono.just("")))
                .flatMap(langEmojiTuple->message.getChannel()
                        .flatMap(channel->channel.createMessage(MessageCreateSpec.builder()
                                .messageReference(message.getId())
                                .allowedMentions(AllowedMentions.suppressAll())
                                .addEmbed(EmbedCreateSpec.builder()
                                    .description(translatedContent)
                                    .addField("From", langEmojiTuple.getT1()+sourceLanguage.getName(), true)
                                    .addField("To", langEmojiTuple.getT2()+targetLanguage.getName(), true)
                                    .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                                    .footer(getCommandSenderName(event), event.getInteraction().getUser().getAvatarUrl())
                                    .build())
                                .build())))
                .then(translateLanguageUsageRepository.setLastLanguageCode(event.getInteraction().getUser().getId().asLong(),
                        targetLanguage.getCode())
                        .onErrorResume(e->Mono.fromRunnable(()->
                            LOGGER.error("Error on saving last language: {}", ThrowableUtil.stackTraceToString(e)))));
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

    private String getCommandSenderName(DeferrableInteractionEvent event) {
        return event.getInteraction().getMember()
                .map(member->member.getDisplayName())
                .orElseGet(()->event.getInteraction().getUser().getGlobalName()
                        .orElseGet(()->event.getInteraction().getUser().getUsername()));
    }

    private Mono<String> unicodeFlagEmoji(TranslateLanguage lang){
        if(lang.getCountryCode() == null) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(UnicodeUtils.unicodeFlagEmoji(lang.getCountryCode()));
    }

}
