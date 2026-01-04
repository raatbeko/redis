package project.redis.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import project.redis.model.Country;
import project.redis.repository.CountryRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "countries")
public class CountryService {

    private final CountryRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String POPULARITY_KEY = "countries:popularity"; // ZSET

    @Cacheable(key = "#id")
    public Country getById(Long id) {
        Country country = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Country not found: " + id));

        // Увеличиваем счётчик популярности атомарно
        redisTemplate.opsForZSet()
                .incrementScore(POPULARITY_KEY, id.toString(), 1);

        return country;
    }

    @Cacheable
    public List<Country> getAll() {
        return repository.findAll();
    }

    // Новый метод: топ N самых популярных стран
    public List<Country> getTopPopular(int limit) {

        Set<ZSetOperations.@NonNull TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULARITY_KEY, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<Long> topIds = tuples.stream()
                .map(tuple -> Long.parseLong(tuple.getValue().toString()))
                .collect(Collectors.toList());

        return repository.findAllById(topIds)
                .stream()
                .sorted((c1, c2) -> {
                    // Сортируем в порядке из Redis (по убыванию счёта)
                    int idx1 = topIds.indexOf(c1.getId());
                    int idx2 = topIds.indexOf(c2.getId());
                    return Integer.compare(idx1, idx2);
                })
                .collect(Collectors.toList());
    }

    // Очистка при изменении/удалении (чтобы не было "зависших" счётчиков)
    @Caching(evict = {
            @CacheEvict(key = "#country.id"),
            @CacheEvict(allEntries = true)
    })
    public Country create(Country country) {
        Country saved = repository.save(country);
        // Можно сразу добавить в ZSET с нулевым счётом
        redisTemplate.opsForZSet().add(POPULARITY_KEY, saved.getId().toString(), 0);
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(key = "#country.id"),
            @CacheEvict(allEntries = true)
    })
    public Country update(Country country) {
        return repository.save(country);
    }

    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(allEntries = true)
    })
    public void delete(Long id) {
        repository.deleteById(id);
        // Удаляем из счётчика популярности
        redisTemplate.opsForZSet().remove(POPULARITY_KEY, id.toString());
    }
}
