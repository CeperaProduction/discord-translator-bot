package me.cepera.discord.bot.translator.discord.translate;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import io.netty.util.internal.ThrowableUtil;
import me.cepera.discord.bot.translator.discord.events.OperationSelectMenuEventHandler;
import me.cepera.discord.bot.translator.model.TranslateLanguage;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.service.TranslateService;
import reactor.core.publisher.Mono;

public abstract class LanguageSelectMenuEventHandler extends OperationSelectMenuEventHandler implements LanguageSelectProvider{

    private static final Logger LOGGER = LogManager.getLogger(LanguageSelectMenuEventHandler.class);

    public static final String OPERATION = "language";

    private final TranslateService translateService;

    private final TranslateLanguageUsageRepository usageRepository;

    private final String operationTag;

    public LanguageSelectMenuEventHandler(TranslateService translateService,
            TranslateLanguageUsageRepository usageRepository, String operationTag) {
        super(OPERATION);
        this.translateService = translateService;
        this.usageRepository = usageRepository;
        this.operationTag = operationTag;
    }

    @Override
    public TranslateService translateService() {
        return translateService;
    }

    @Override
    public TranslateLanguageUsageRepository usageRepository() {
        return usageRepository;
    }

    @Override
    protected Mono<Void> handleOperation(SelectMenuInteractionEvent event, List<String> operands) {
        if(operands.isEmpty() || !operands.get(0).equals(operationTag)) {
            return Mono.empty();
        }

        List<String> nextOperands = new ArrayList<>(operands);
        nextOperands.remove(0);

        String selectedValue = event.getValues().get(0);

        if(selectedValue.startsWith(PAGE_VALUE_PREFIX)) {
            return event.deferEdit()
                    .then(sendLanguateChooseMessage(event,
                            Integer.parseInt(selectedValue.substring(PAGE_VALUE_PREFIX.length())),
                            nextOperands));
        }

        return translateService.getLanguage(selectedValue)
                .switchIfEmpty(Mono.error(()->new IllegalArgumentException("Can't find language with code '"+selectedValue+"'")))
                .flatMap(language->usageRepository.setLastLanguageCode(event.getInteraction().getUser().getId().asLong(), language.getCode())
                        .onErrorResume(e->Mono.fromRunnable(()->LOGGER.error("Failed to save last used language for user {}: {}",
                                event.getInteraction().getUser().getTag(),
                                ThrowableUtil.stackTraceToString(e))))
                        .then(Mono.just(language)))
                .flatMap(language->handleLanguageSelection(event, language, nextOperands));
    }

    protected abstract Mono<Void> handleLanguageSelection(SelectMenuInteractionEvent event,
            TranslateLanguage language, List<String> operands);

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public String getSelectorId(List<String> operands) {
        List<String> extendedOperands = new ArrayList<>(operands);
        extendedOperands.add(0, operationTag);
        return super.getSelectorId(extendedOperands);
    }


}
