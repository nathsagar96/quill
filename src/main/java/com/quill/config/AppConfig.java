package com.quill.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    AppProperties.class,
    CorsProperties.class,
    RateLimitProperties.class
})
public class AppConfig {}
