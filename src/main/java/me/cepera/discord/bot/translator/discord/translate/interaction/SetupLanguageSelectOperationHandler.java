package me.cepera.discord.bot.translator.discord.translate.interaction;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.cepera.discord.bot.translator.discord.translate.DiscordTranslatorBot;
import me.cepera.discord.bot.translator.discord.translate.LanguageSelectMenuEventHandler;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public class SetupLanguageSelectOperationHandler extends LanguageSelectMenuEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(SetupLanguageSelectOperationHandler.class);

    public static final String OPERATION_SETUP_LANGUAGE = "setup";

    private final DiscordClient discordClient;

    public SetupLanguageSelectOperationHandler(TranslateService translateService,
            TranslateLanguageUsageRepository usageRepository, DiscordClient discordClient) {
        super(translateService, usageRepository, OPERATION_SETUP_LANGUAGE);
        this.discordClient = discordClient;
    }

    @Override
    protected Mono<Void> handleLanguageSelection(SelectMenuInteractionEvent event, TranslateLanguage language,
            List<String> operands) {
        return event.deferEdit()
                .withEphemeral(true)
                .then(setupLanguageCommand(event, language));
    }

    private Mono<Void> setupLanguageCommand(DeferrableInteractionEvent event, TranslateLanguage language){
        String commandName = DiscordTranslatorBot.MESSAGE_COMMAND_TRANSLATE + " " + language.getName();
        return event.getInteraction().getGuild()
                .flatMap(guild->discordClient.getApplicationId()
                        .flatMap(appId->discordClient.getApplicationService()
                                .getGuildApplicationCommands(appId, guild.getId().asLong())
                                .filter(cmd->cmd.name().equals(commandName))
                                .take(1)
                                .singleOrEmpty()
                                .switchIfEmpty(Mono.defer(()->discordClient.getApplicationService()
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
                                        .then(Mono.empty())))
                                .flatMap(cmd->discordClient.getApplicationService()
                                        .deleteGuildApplicationCommand(appId, guild.getId().asLong(), cmd.id().asLong())
                                        .then(Mono.fromRunnable(()->LOGGER.info("{} removed default lang {} from server {}",
                                                event.getInteraction().getUser().getTag(),
                                                language.getCode(),
                                                guild.getId().asLong())))
                                        .then(event.editReply()
                                                .withContentOrNull(commandName+" default command removed.")
                                                .withComponentsOrNull(Collections.emptyList()))
                                        .then())));
    }

}
