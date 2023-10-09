package me.cepera.discord.bot.translator.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AutoTranslatingChannelRepository {

    Flux<AutoTranslatingChannel> getAllChannels();

    Mono<AutoTranslatingChannel> getChannel(long channelId);

    Mono<Void> setChannel(AutoTranslatingChannel channel);

    Mono<Void> removeChannel(long channelId);

}
