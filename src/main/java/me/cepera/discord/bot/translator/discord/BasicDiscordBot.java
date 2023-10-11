package me.cepera.discord.bot.translator.discord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class BasicDiscordBot implements DiscordBot{

    private static final Logger LOGGER = LogManager.getLogger(BasicDiscordBot.class);

    protected final DiscordBotConfig config;

    private final DiscordClient discordClient;

    public BasicDiscordBot(DiscordBotConfig config) {
        this.config = config;
        String botApiKey = config.getKey();

        if(botApiKey == null || botApiKey.isEmpty()) {
            throw new IllegalArgumentException("Discord bot key must be provided.");
        }

        this.discordClient = DiscordClient.create(botApiKey);
    }

    @Override
    public void start() {
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
            .doOnNext(commandData->LOGGER.info("Global {} {} registered.",
                            commandData.type().toOptional().map(type->type == 1).orElse(false) ? "command" : "interaction",
                            commandData.name()))
            .subscribe();
    }

    protected Flux<ApplicationCommandRequest> commandsToRegister(){
        return Flux.empty();
    }

    protected void configureGatewayClient(GatewayDiscordClient client) {

    }

    protected Mono<Void> handleReadyEvent(ReadyEvent event){
        return Mono.fromRunnable(()->LOGGER.info("Discord bot logged in as {}", event.getSelf().getUsername()));
    }

}
