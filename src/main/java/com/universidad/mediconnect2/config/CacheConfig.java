package com.universidad.mediconnect2.config;

import java.time.Duration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    @Profile({"prod","docker"})
    RedisCacheManagerBuilderCustomizer redisCacheTtls() {
        return builder -> builder
                .withCacheConfiguration("doctorsBySpecialty",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10)).disableCachingNullValues())
                .withCacheConfiguration("validatedDrugs",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(24)).disableCachingNullValues());
    }
}
