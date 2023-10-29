package me.cepera.discord.bot.translator.discord.translate.interaction;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import me.cepera.discord.bot.translator.discord.events.DiscordEventHandler;
import reactor.core.publisher.Mono;

public class HideTranslationButtonInteractionHandler implements DiscordEventHandler<ButtonInteractionEvent>{

    public static final String BUTTON_ID = "button-translation-hide";

    @Override
    public Class<ButtonInteractionEvent> getEventClass() {
        return ButtonInteractionEvent.class;
    }

    @Override
    public Mono<Void> handleEvent(ButtonInteractionEvent event) {
        if(!BUTTON_ID.equals(event.getCustomId())) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(event.getMessage())
                .flatMap(Message::delete);
    }

}
