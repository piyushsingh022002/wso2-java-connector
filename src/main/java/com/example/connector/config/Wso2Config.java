package com.example.connector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wso2")
public class Wso2Config {
    private String baseUrl;
    private String scimUsersEndpoint;
    private String username;
    private String password;
    private String pageSize;
};
