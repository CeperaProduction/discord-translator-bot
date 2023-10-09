package me.cepera.discord.bot.translator.discord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public abstract class BasicDiscordBot implements DiscordBot{

    private static final Logger LOGGER = LogManager.getLogger(BasicDiscordBot.class);

    private final Scheduler botActionsScheduler = Schedulers.newBoundedElastic(Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE, Integer.MAX_VALUE, "discord-bot-actions");

    protected final DiscordBotConfig config;

    private DiscordClient discordClient;

    public BasicDiscordBot(DiscordBotConfig config) {
        this.config = config;
    }

    @Override
    public void start() {

        String botApiKey = config.getKey();

        if(botApiKey == null || botApiKey.isEmpty()) {
            throw new IllegalArgumentException("Discord bot key must be provided.");
        }

        this.discordClient = DiscordClient.create(botApiKey);
        this.discordClient.withGateway(client->{

                client.getEventDispatcher().on(ReadyEvent.class)
                    .flatMap(this::handleReadyEvent)
                    .subscribe();

                configureGatewayClient(client);

                registerCommands(client);


                return client.onDisconnect();
            })
            .block();

    }

    @Override
    public DiscordClient getDiscordClient() {
        return discordClient;
    }

    protected void registerCommands(GatewayDiscordClient client) {
        client.getRestClient().getApplicationId()
            .flatMapMany(appId->commandsToRegister()
                    .flatMap(command->client.getRestClient().getApplicationService()
                            .createGlobalApplicationCommand(appId, command)))
            .doOnNext(commandData->LOGGER.info("Global command {} registered.", commandData.name()))
            .subscribe();
    }

    protected Flux<ApplicationCommandRequest> commandsToRegister(){
        return Flux.empty();
    }

    protected void configureGatewayClient(GatewayDiscordClient client) {

        client.on(ChatInputInteractionEvent.class)
            .publishOn(botActionsScheduler)
            .flatMap(event->this.handleChatInputInteractionEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling chat interaction", e);
                        return Mono.empty();
                    }))
            .subscribe();

        client.on(MessageInteractionEvent.class)
            .publishOn(botActionsScheduler)
            .flatMap(event->this.handleMessageInteractionEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling message interaction", e);
                        return Mono.empty();
                    }))
            .subscribe();

        client.on(ChatInputAutoCompleteEvent.class)
            .publishOn(botActionsScheduler)
            .flatMap(event->this.handleChatInputAutocompleteEvent(event)
                    .onErrorResume(e->{
                        LOGGER.error("Error on handling autocompletion", e);
                        return Mono.empty();
                    }))
            .subscribe();

    }

    protected Mono<Void> handleReadyEvent(ReadyEvent event){
        return Mono.fromRunnable(()->LOGGER.info("Discord bot logged in as {}", event.getSelf().getUsername()));
    }

    protected Mono<Void> handleChatInputInteractionEvent(ChatInputInteractionEvent event){
        return Mono.empty();
    }

    protected Mono<Void> handleChatInputAutocompleteEvent(ChatInputAutoCompleteEvent event){
        return Mono.empty();
    }

    protected Mono<Void> handleMessageInteractionEvent(MessageInteractionEvent event){
        return Mono.empty();
    }

}
