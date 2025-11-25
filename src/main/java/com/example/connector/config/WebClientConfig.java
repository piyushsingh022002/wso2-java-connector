package com.example.connector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${cymmetri.base-url}")
    private String baseUrl;

    @Value("${cymmetri.username}")
    private String username;

    @Value("${cymmetri.password}")
    private String password;

    @Bean
    public WebClient cymmetriClient() {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(15))
                .wiretap("http-client-logger", // full request/response logging
                        io.netty.handler.logging.LogLevel.DEBUG,
                        AdvancedByteBufFormat.TEXTUAL);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    String auth = username + ":" + password;
                    String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.set("Authorization", "Basic " + encoded);
                })
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(config -> config.defaultCodecs().maxInMemorySize(5_000_000))
                                .build()
                )
                .build();
    }
}
