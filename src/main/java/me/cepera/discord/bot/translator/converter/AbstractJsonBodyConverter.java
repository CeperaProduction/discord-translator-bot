package me.cepera.discord.bot.translator.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

public abstract class AbstractJsonBodyConverter<T> implements BodyConverter<T> {

    protected final ObjectMapper objectMapper;

    public AbstractJsonBodyConverter() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    protected Mono<T> read(Class<T> clazz, byte[] bytes){
        return Mono.fromCallable(()->objectMapper.readValue(bytes, clazz));
    }

    @Override
    public Mono<byte[]> write(T object) {
        return Mono.fromCallable(()->objectMapper.writeValueAsBytes(object));
    }

}
