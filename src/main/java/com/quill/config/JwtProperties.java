package com.quill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "quill.jwt")
public record JwtProperties(Path keyStore, Duration expiration) {}
