package com.quill.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class CacheControlFilter {

    private static final PathPatternRequestMatcher CATEGORIES =
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/categories/**");
    private static final PathPatternRequestMatcher TAGS =
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/tags/**");
    private static final PathPatternRequestMatcher POSTS_LIST =
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/posts");
    private static final PathPatternRequestMatcher POSTS_DETAIL =
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/posts/**");

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> cacheControlHeaderFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !"GET".equals(request.getMethod());
            }

            @Override
            protected void doFilterInternal(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull FilterChain chain)
                    throws ServletException, IOException {

                if (CATEGORIES.matches(request) || TAGS.matches(request)) {
                    response.setHeader(
                            "Cache-Control",
                            CacheControl.maxAge(3600, TimeUnit.SECONDS)
                                    .cachePublic()
                                    .getHeaderValue());

                } else if (POSTS_LIST.matches(request)) {
                    response.setHeader(
                            "Cache-Control",
                            CacheControl.maxAge(60, TimeUnit.SECONDS)
                                    .cachePublic()
                                    .staleWhileRevalidate(300, TimeUnit.SECONDS)
                                    .getHeaderValue());

                } else if (POSTS_DETAIL.matches(request)) {
                    response.setHeader(
                            "Cache-Control",
                            CacheControl.maxAge(60, TimeUnit.SECONDS)
                                    .cachePrivate()
                                    .getHeaderValue());
                }

                chain.doFilter(request, response);
            }
        };

        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/api/categories/*", "/api/tags/*", "/api/posts", "/api/posts/*", "/api/posts/slug/*");
        bean.setName("cacheControlHeaderFilter");
        bean.setOrder(1);
        return bean;
    }
}
