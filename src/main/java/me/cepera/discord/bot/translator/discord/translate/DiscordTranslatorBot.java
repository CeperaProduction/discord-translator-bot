package me.cepera.discord.bot.translator.discord.translate;

import javax.inject.Inject;

import discord4j.discordjson.json.ApplicationCommandRequest;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import me.cepera.discord.bot.translator.discord.EventDrivenDiscordBot;
import me.cepera.discord.bot.translator.discord.translate.auto.CreatedMessagesTranslateHandler;
import me.cepera.discord.bot.translator.discord.translate.auto.UpdatedMessagesTranslateHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.AutoTranslateChatInputInteractionHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.AutoTranslateSelectLanguageOperationHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.MessageInteractionTranslateHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.SetupChatInputInteractionHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.SetupLanguageSelectOperationHandler;
import me.cepera.discord.bot.translator.discord.translate.interaction.TranslateLanguageSelectOperationHandler;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Flux;

public class DiscordTranslatorBot extends EventDrivenDiscordBot{

    public static final String COMMAND_SETUP = "setup";
    public static final String COMMAND_AUTO = "auto";

    public static final String MESSAGE_COMMAND_TRANSLATE = "Translate to";

    @Inject
    public DiscordTranslatorBot(DiscordBotConfig config) {
        super(config);
    }

    @Inject
    void setup(TranslateService translateService,
            TranslateLanguageUsageRepository languageUsageRepository,
            AutoTranslatingChannelRepository autoTranslatingChannelRepository) {

        SetupLanguageSelectOperationHandler setupLanguageSelectOperationHandler = new SetupLanguageSelectOperationHandler(
                translateService, languageUsageRepository, getDiscordClient());

        SetupChatInputInteractionHandler setupChatInputInteractionHandler = new SetupChatInputInteractionHandler(
                translateService, languageUsageRepository, setupLanguageSelectOperationHandler);

        TranslateLanguageSelectOperationHandler translateLanguageSelectOperationHandler = new TranslateLanguageSelectOperationHandler(
                translateService, languageUsageRepository);

        MessageInteractionTranslateHandler messageInteractionTranslateHandler = new MessageInteractionTranslateHandler(
                translateService, languageUsageRepository, translateLanguageSelectOperationHandler);

        AutoTranslateSelectLanguageOperationHandler autoTranslateSelectLanguageOperationHandler = new AutoTranslateSelectLanguageOperationHandler(
                translateService, languageUsageRepository, autoTranslatingChannelRepository);

        AutoTranslateChatInputInteractionHandler autoTranslateChatInputInteractionHandler = new AutoTranslateChatInputInteractionHandler(
                translateService, languageUsageRepository, autoTranslatingChannelRepository, autoTranslateSelectLanguageOperationHandler);

        CreatedMessagesTranslateHandler createdMessagesTranslateHandler = new CreatedMessagesTranslateHandler(
                translateService, autoTranslatingChannelRepository, getDiscordClient());

        UpdatedMessagesTranslateHandler updatedMessagesTranslateHandler = new UpdatedMessagesTranslateHandler(
                translateService, autoTranslatingChannelRepository, getDiscordClient());

        addEventHandler(messageInteractionTranslateHandler);
        addEventHandler(setupChatInputInteractionHandler);
        addEventHandler(setupLanguageSelectOperationHandler);
        addEventHandler(translateLanguageSelectOperationHandler);
        addEventHandler(autoTranslateChatInputInteractionHandler);
        addEventHandler(autoTranslateSelectLanguageOperationHandler);
        addEventHandler(createdMessagesTranslateHandler);
        addEventHandler(updatedMessagesTranslateHandler);
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

}
