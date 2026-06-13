package com.quill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, OpenApi openapi) {

    public record OpenApi(String serverUrl) {}
}
