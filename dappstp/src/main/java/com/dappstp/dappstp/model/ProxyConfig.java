package com.dappstp.dappstp.model;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "proxy")
@Data
public class ProxyConfig {
    private boolean enabled;
    private String provider;
    private String apiKey;
    private boolean renderJs;
    private boolean premium;
    private int retries;
    private int timeout;
}

