package me.cepera.discord.bot.translator.service.remote;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

public interface RemoteService {

    Logger LOGGER = LogManager.getLogger(RemoteService.class);

    HttpClient DEFAULT_HTTP_CLIENT = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30));

    default HttpClient httpClient() {
        return DEFAULT_HTTP_CLIENT;
    }

    default Mono<byte[]> get(URI uri){
        return get(uri, Collections.emptyMap());
    }

    default Mono<byte[]> get(URI uri, Map<String, String> headersMap){
        return httpClient()
                .headers(headers->headersMap.forEach((key, value)->headers.add(key, value)))
                .get()
                .uri(uri)
                .responseSingle(this::resolveResponse);
    }

    default Mono<byte[]> post(URI uri, byte[] requestBody){
        return post(uri, Collections.emptyMap(), requestBody);
    }

    default Mono<byte[]> post(URI uri, Map<String, String> headersMap, byte[] requestBody){
        return httpClient()
                .headers(headers->headersMap.forEach((key, value)->headers.add(key, value)))
                .post()
                .uri(uri)
                .send((req, out)->out.sendByteArray(Mono.just(requestBody)))
                .responseSingle(this::resolveResponse);
    }

    default Mono<byte[]> resolveResponse(HttpClientResponse response, ByteBufMono content){
        return Mono.just(response)
                .flatMap(r->content.asByteArray()
                        .doOnNext(bytes->LOGGER.debug("Remote service response: {}", new String(bytes)))
                        .doOnNext(bytes->{
                            if(r.status().code() >= 400) {
                                LOGGER.error("Remote service responsed with bad status {}. Response body: {}", response.status(), new String(bytes));
                                throw new IllegalStateException("Remote service responsed with bad status "+response.status());
                            }
                        })
                        .switchIfEmpty(Mono.fromRunnable(()->LOGGER.debug("Remote service response has no body"))));
    }

}
