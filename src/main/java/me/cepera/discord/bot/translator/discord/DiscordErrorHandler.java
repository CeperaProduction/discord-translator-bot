package me.cepera.discord.bot.translator.discord;

import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import io.netty.util.internal.ThrowableUtil;
import reactor.core.publisher.Mono;

public interface DiscordErrorHandler {

    Logger logger();

    default Mono<Void> sendErrorMessage(DeferrableInteractionEvent event, Throwable e){
        logger().error("An unexpected error has occurred while processing interaction. {}",
                ThrowableUtil.stackTraceToString(e));
        String message = "An unexpected error has occurred. "+e.toString();
        if(message.length() > 1980) {
            message = message.substring(0, 1980)+"...";
        }
        return event.editReply()
            .withContentOrNull(message)
            .then();
    }

}
