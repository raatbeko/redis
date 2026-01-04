# Countries Directory with Redis Caching

A simple pet project built with Spring Boot that demonstrates practical and advanced usage of **Redis** - not just as a basic cache, but as a powerful real-time analytics tool.

### Features

- CRUD operations for countries (code, name, capital, currency)
- Caching with `@Cacheable` / `@CacheEvict`
- Custom TTL configuration for cache entries
- JSON serialization (easy to inspect in Redis)
- Logging of cache hits and misses
- **Advanced feature**: Real-time popularity leaderboard using Redis Sorted Set (ZSET)

### How the Popularity Leaderboard Works

Every time a country is viewed via `GET /api/countries/{id}`:
- The view counter for that country is atomically incremented in Redis
- A new endpoint `GET /api/countries/top?limit=10` instantly returns the most viewed countries - **no database load**

This is a classic example of using Redis as a **real-time analytics store** in addition to caching.

### Tech Stack

- Spring Boot 3 (or 4)
- Spring Data JPA (H2 in dev / PostgreSQL in Docker)
- Spring Cache + Redis
- Redis Sorted Set for leaderboard
- Lombok
- Testcontainers (in tests)
- Docker Compose (app + Redis + PostgreSQL)
