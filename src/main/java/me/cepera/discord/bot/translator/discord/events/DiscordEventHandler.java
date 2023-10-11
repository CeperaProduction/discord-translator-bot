package me.cepera.discord.bot.translator.discord.events;

import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;

public interface DiscordEventHandler<E extends Event> {

    Class<E> getEventClass();

    Mono<Void> handleEvent(E event);

}
