package com.quill.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("categories", "category", "tags", "tag");
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.HOURS));
        return manager;
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ShallowEtagHeaderFilter());
        bean.addUrlPatterns("/api/categories/*", "/api/tags/*", "/api/posts", "/api/posts/*", "/api/posts/slug/*");
        bean.setName("shallowEtagHeaderFilter");
        return bean;
    }
}
