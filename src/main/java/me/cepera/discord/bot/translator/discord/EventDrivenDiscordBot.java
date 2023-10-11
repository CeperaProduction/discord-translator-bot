package me.cepera.discord.bot.translator.discord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import io.netty.util.internal.ThrowableUtil;
import me.cepera.discord.bot.translator.config.DiscordBotConfig;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class EventDrivenDiscordBot extends BasicDiscordBot{

    private static final Logger LOGGER = LogManager.getLogger(EventDrivenDiscordBot.class);

    private final Scheduler botActionsScheduler = Schedulers.newBoundedElastic(
            Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE, Integer.MAX_VALUE, "discord-bot-actions");

    private final List<DiscordEventHandler<Event>> eventHandlers = new CopyOnWriteArrayList<>();

    private boolean configured;

    public EventDrivenDiscordBot(DiscordBotConfig config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    protected void addEventHandler(DiscordEventHandler<?> handler) {
        if(configured) {
            throw new IllegalStateException("Gateway client already configured.");
        }
        this.eventHandlers.add((DiscordEventHandler<Event>)handler);
    }

    @Override
    protected final void configureGatewayClient(GatewayDiscordClient client) {
        this.configured = true;
        Flux.fromIterable(eventHandlers)
            .flatMap(eventHandler->client.on(eventHandler.getEventClass())
                .publishOn(botActionsScheduler)
                .flatMap(event->eventHandler.handleEvent(event)
                        .onErrorResume(e->{
                            LOGGER.error("Error on handling {}: {}",
                                    eventHandler.getEventClass().getSimpleName(), ThrowableUtil.stackTraceToString(e));
                            return Mono.empty();
                        })))
            .subscribe();
        gatewayClientConfigured(client);
    }

    protected final void gatewayClientConfigured(GatewayDiscordClient client) {

    }

}
