package project.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@EnableCaching
@Configuration
public class RedisCacheConfig {

    @Value("${cache.countries-ttl-minutes:10}")
    private long countriesTtlMinutes;

    @Bean
    @Primary
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {

        // Дефолтная конфигурация
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJacksonJsonRedisSerializer(objectMapper)));

        // Специфическая конфигурация для кэша "countries"
        RedisCacheConfiguration countriesConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(countriesTtlMinutes));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("countries", countriesConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    // Логирование hit/miss
    @Bean
    public CacheManager customizerCacheManager(CacheManager cacheManager) {
        if (cacheManager instanceof RedisCacheManager redisCacheManager) {
            return new LoggingRedisCacheManager(redisCacheManager);
        }
        return cacheManager;
    }

    // Обертка для логирования
    private static class LoggingRedisCacheManager implements CacheManager {
        private final RedisCacheManager delegate;

        LoggingRedisCacheManager(RedisCacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache != null) {
                return new LoggingCache(cache, name);
            }
            return null;
        }

        @Override
        public Collection<String> getCacheNames() {
            return List.of();
        }
    }

    private static class LoggingCache implements Cache {
        private final Cache delegate;
        private final String cacheName;

        LoggingCache(Cache delegate, String cacheName) {
            this.delegate = delegate;
            this.cacheName = cacheName;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper wrapper = delegate.get(key);
            if (wrapper != null) {
                log.info("CACHE HIT  -> cache={}, key={}", cacheName, key);
            } else {
                log.info("CACHE MISS -> cache={}, key={}", cacheName, key);
            }
            return wrapper;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return delegate.get(key, type);
        }

        @Override
        public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }
}
