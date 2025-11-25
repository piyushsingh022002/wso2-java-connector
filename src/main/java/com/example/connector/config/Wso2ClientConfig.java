package com.example.connector.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class Wso2ClientConfig {

    @Value("${wso2.base-url}")
    private String baseUrl;

    @Value("${wso2.username}")
    private String username;

    @Value("${wso2.password}")
    private String password;

    @Bean
    public WebClient wso2Client() {

        String basicAuth = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .build();
    }
}
