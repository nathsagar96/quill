package com.quill.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration(proxyBeanMethods = false)
public class Bucket4jConfig {

    @Bean
    LettuceBasedProxyManager<byte[]> lettuceBasedProxyManager(RedisConnectionFactory connectionFactory) {
        var lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        return Bucket4jLettuce.casBasedBuilder((RedisClient) lettuceFactory.getNativeClient())
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(30)))
                .build();
    }
}
