package com.quill.filter;

import com.quill.config.RateLimitProperties;
import com.quill.config.RateLimitProperties.BandwidthConfig;
import com.quill.config.RateLimitProperties.EndpointConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RateLimitingFilter {

    private static final List<String> SKIP_PATHS = List.of("/actuator", "/swagger-ui", "/v3/api-docs", "/api-docs");

    private final RateLimitProperties properties;
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    public RateLimitingFilter(RateLimitProperties properties, LettuceBasedProxyManager<byte[]> proxyManager) {
        this.properties = properties;
        this.proxyManager = proxyManager;
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> rateLimitingFilterRegistration(ObjectMapper objectMapper) {
        OncePerRequestFilter filter = new OncePerRequestFilter() {

            @Override
            protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
                if (!properties.enabled()) {
                    return true;
                }
                String path = request.getRequestURI();
                return SKIP_PATHS.stream().anyMatch(path::startsWith);
            }

            @Override
            protected void doFilterInternal(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull FilterChain chain)
                    throws ServletException, IOException {

                EndpointConfig config = findMatchingConfig(request);
                if (config == null) {
                    chain.doFilter(request, response);
                    return;
                }

                String cacheKey = resolveCacheKey(request, config);
                Bucket bucket =
                        proxyManager.getProxy(cacheKey.getBytes(StandardCharsets.UTF_8), () -> toConfiguration(config));

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
        };

        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/*");
        bean.setName("rateLimitingFilter");
        bean.setOrder(2);
        return bean;
    }

    private BucketConfiguration toConfiguration(EndpointConfig config) {
        List<BandwidthConfig> bandwidths = config.bandwidths();
        if (bandwidths.isEmpty() && properties.defaults() != null) {
            bandwidths = List.of(properties.defaults());
        }
        var builder = BucketConfiguration.builder();
        for (BandwidthConfig bw : bandwidths) {
            Duration period = bw.toDuration();
            builder.addLimit(limit -> switch (bw.refillSpeed()) {
                case GREEDY -> limit.capacity(bw.capacity()).refillGreedy(bw.refill(), period);
                case INTERVAL -> limit.capacity(bw.capacity()).refillIntervally(bw.refill(), period);
            });
        }
        return builder.build();
    }
}
