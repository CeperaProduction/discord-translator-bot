package me.cepera.discord.bot.translator.discord.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import reactor.core.publisher.Mono;

public abstract class OperationSelectMenuEventHandler implements DiscordEventHandler<SelectMenuInteractionEvent> {

    private static final String OPERANDS_DELEMITER = "-";

    private final String operationTag;

    public OperationSelectMenuEventHandler(String operationTag) {
        this.operationTag = operationTag;
    }

    @Override
    public Class<SelectMenuInteractionEvent> getEventClass() {
        return SelectMenuInteractionEvent.class;
    }

    @Override
    public Mono<Void> handleEvent(SelectMenuInteractionEvent event) {
        if(event.getCustomId().isEmpty()) {
            return Mono.empty();
        }
        if(event.getValues().isEmpty()) {
            return Mono.empty();
        }
        List<String> operands = new ArrayList<String>(Arrays.asList(event.getCustomId().split(OPERANDS_DELEMITER)));
        String tag = operands.get(0);
        if(!tag.equals(operationTag)) {
            return Mono.empty();
        }
        operands.remove(0);

        return handleOperation(event, Collections.unmodifiableList(operands));
    }

    protected abstract Mono<Void> handleOperation(SelectMenuInteractionEvent event, List<String> operands);

    protected String getSelectorId(List<String> operands) {
        List<String> idSplit = new ArrayList<>(operands);
        idSplit.add(0, operationTag);
        return String.join(OPERANDS_DELEMITER, idSplit);
    }

}
