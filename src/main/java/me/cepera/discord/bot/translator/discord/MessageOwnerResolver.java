package me.cepera.discord.bot.translator.discord;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface MessageOwnerResolver {

    DiscordClient discordClient();

    default Mono<Boolean> isMyMessage(Message message){
        return discordClient().getSelf().zipWith(Mono.justOrEmpty(message.getAuthor()))
            .filter(tuple->tuple.getT1().id().asLong() == tuple.getT2().getId().asLong())
            .map(tuple->Boolean.TRUE)
            .switchIfEmpty(Mono.just(Boolean.FALSE));
    }

    default Mono<Boolean> isNotMyMessage(Message message){
        return isMyMessage(message).map(res->!res);
    }

    default Mono<Message> myMessageOrEmpty(Message message){
        return Mono.just(message).filterWhen(this::isMyMessage);
    }

    default Mono<Message> notMyMessageOrEmpty(Message message){
        return Mono.just(message).filterWhen(this::isNotMyMessage);
    }

}
