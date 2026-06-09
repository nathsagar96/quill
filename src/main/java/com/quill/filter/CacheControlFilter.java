package com.quill.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class CacheControlFilter {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> cacheControlHeaderFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {

                if (!"GET".equals(request.getMethod())) {
                    chain.doFilter(request, response);
                    return;
                }

                String path = request.getRequestURI();

                if (path.startsWith("/api/categories") || path.startsWith("/api/tags")) {
                    response.setHeader(
                            "Cache-Control",
                            CacheControl.maxAge(3600, TimeUnit.SECONDS)
                                    .cachePublic()
                                    .getHeaderValue());

                } else if (path.startsWith("/api/posts")) {
                    String rest = path.substring("/api/posts".length());
                    if (rest.isEmpty() || rest.equals("/")) {
                        response.setHeader(
                                "Cache-Control",
                                CacheControl.maxAge(60, TimeUnit.SECONDS)
                                        .cachePublic()
                                        .staleWhileRevalidate(300, TimeUnit.SECONDS)
                                        .getHeaderValue());
                    } else {
                        response.setHeader(
                                "Cache-Control",
                                CacheControl.maxAge(60, TimeUnit.SECONDS)
                                        .cachePrivate()
                                        .getHeaderValue());
                    }
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
