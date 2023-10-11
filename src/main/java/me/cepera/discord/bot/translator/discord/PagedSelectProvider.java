package me.cepera.discord.bot.translator.discord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagedSelectProvider {

    String PAGE_VALUE_PREFIX = "page-next-";

    int MAX_OPTIONS_PER_PAGE = 15;

    default Mono<SelectMenu> getSelector(Flux<Option> optionSource, String placeholder, int startIndex, List<String> operands){
        return optionSource
                .skip(startIndex)
                .take(MAX_OPTIONS_PER_PAGE)
                .collectList()
                .map(options->{

                    int nextIndex = startIndex + options.size();

                    Set<String> added = new HashSet<>();
                    List<Option> finalOptions = new ArrayList<>();
                    options.stream().filter(opt->added.add(opt.getValue()))
                        .forEach(finalOptions::add);

                    if(startIndex != 0) {
                        finalOptions.add(0, Option.of("Previous...", PAGE_VALUE_PREFIX+Math.max(0, startIndex-MAX_OPTIONS_PER_PAGE)));
                    }
                    if(options.size() == MAX_OPTIONS_PER_PAGE) {
                        finalOptions.add(Option.of("Next...", PAGE_VALUE_PREFIX+nextIndex));
                    }

                    String selectorId = getSelectorId(operands);

                    SelectMenu selector = SelectMenu.of(selectorId, finalOptions)
                            .withMaxValues(1)
                            .withMinValues(1)
                            .withPlaceholder(placeholder);

                    return selector;
                });
    }

    String getSelectorId(List<String> operands);

}
