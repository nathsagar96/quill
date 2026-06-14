package com.quill.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, BandwidthConfig defaults, List<EndpointConfig> endpoints) {

    public RateLimitProperties {
        if (endpoints == null) {
            endpoints = List.of();
        }
    }

    public record EndpointConfig(
            String pattern, List<String> httpMethods, CacheKeyStrategy cacheKey, List<BandwidthConfig> bandwidths) {

        public EndpointConfig {
            if (httpMethods == null) {
                httpMethods = List.of();
            }
            if (cacheKey == null) {
                cacheKey = CacheKeyStrategy.IP;
            }
            if (bandwidths == null) {
                bandwidths = List.of();
            }
        }
    }

    public record BandwidthConfig(
            @Positive int capacity, @Positive int refill, TimeUnit timeUnit, RefillSpeed refillSpeed) {

        public BandwidthConfig {
            if (timeUnit == null) {
                timeUnit = TimeUnit.MINUTES;
            }
            if (refillSpeed == null) {
                refillSpeed = RefillSpeed.GREEDY;
            }
        }

        public Duration toDuration() {
            return switch (timeUnit) {
                case SECONDS -> Duration.ofSeconds(refill);
                case MINUTES -> Duration.ofMinutes(refill);
                case HOURS -> Duration.ofHours(refill);
            };
        }
    }

    public enum CacheKeyStrategy {
        IP,
        USERNAME,
        COMBINED
    }

    public enum TimeUnit {
        SECONDS,
        MINUTES,
        HOURS
    }

    public enum RefillSpeed {
        GREEDY,
        INTERVAL
    }
}
