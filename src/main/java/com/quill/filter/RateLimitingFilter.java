package com.quill.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quill.config.RateLimitProperties;
import com.quill.config.RateLimitProperties.BandwidthConfig;
import com.quill.config.RateLimitProperties.EndpointConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class RateLimitingFilter {

    private static final List<String> SKIP_PATHS = List.of("/actuator", "/swagger-ui", "/v3/api-docs", "/api-docs");

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> bucketCache;

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
        this.bucketCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> rateLimitingFilter(ObjectMapper objectMapper) {
        OncePerRequestFilter filter = new OncePerRequestFilter() {

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                if (!properties.enabled()) {
                    return true;
                }
                String path = request.getRequestURI();
                return SKIP_PATHS.stream().anyMatch(path::startsWith);
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {

                EndpointConfig config = findMatchingConfig(request);
                if (config == null) {
                    chain.doFilter(request, response);
                    return;
                }

                String cacheKey = resolveCacheKey(request, config);
                Bucket bucket = bucketCache.get(cacheKey, k -> createBucket(config));

                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                    chain.doFilter(request, response);
                } else {
                    long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
                    response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfter));
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/problem+json");

                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.TOO_MANY_REQUESTS, "Too many requests - rate limit exceeded");
                    problem.setTitle("RateLimitExceeded");
                    problem.setType(URI.create("about:blank"));
                    problem.setProperty("timestamp", Instant.now().toString());

                    objectMapper.writeValue(response.getWriter(), problem);
                }
            }

            private EndpointConfig findMatchingConfig(HttpServletRequest request) {
                var endpoints = properties.endpoints();
                if (endpoints == null || endpoints.isEmpty()) {
                    return null;
                }
                for (EndpointConfig config : endpoints) {
                    var matcher = PathPatternRequestMatcher.pathPattern(config.pattern());
                    if (!matcher.matches(request)) {
                        continue;
                    }
                    var methods = config.httpMethods();
                    if (!methods.isEmpty()
                            && methods.stream().noneMatch(m -> m.equalsIgnoreCase(request.getMethod()))) {
                        continue;
                    }
                    return config;
                }
                return null;
            }

            private String resolveCacheKey(HttpServletRequest request, EndpointConfig config) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String ip = request.getRemoteAddr();
                String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                        ? auth.getName()
                        : null;

                return switch (config.cacheKey()) {
                    case IP -> ip;
                    case USERNAME -> username != null ? username : ip;
                    case COMBINED -> username != null ? ip + ":" + username : ip;
                };
            }

            private Bucket createBucket(EndpointConfig config) {
                List<BandwidthConfig> bandwidths = config.bandwidths();
                if (bandwidths.isEmpty() && properties.defaults() != null) {
                    bandwidths = List.of(properties.defaults());
                }
                var builder = Bucket.builder();
                for (BandwidthConfig bw : bandwidths) {
                    builder.addLimit(toBandwidth(bw));
                }
                return builder.build();
            }

            private Bandwidth toBandwidth(BandwidthConfig config) {
                Duration period = config.toDuration();
                return switch (config.refillSpeed()) {
                    case GREEDY ->
                        Bandwidth.builder()
                                .capacity(config.capacity())
                                .refillGreedy(config.refill(), period)
                                .build();
                    case INTERVAL ->
                        Bandwidth.builder()
                                .capacity(config.capacity())
                                .refillIntervally(config.refill(), period)
                                .build();
                };
            }
        };

        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/*");
        bean.setName("rateLimitingFilter");
        bean.setOrder(2);
        return bean;
    }
}
