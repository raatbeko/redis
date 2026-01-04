package project.redis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.redis.model.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
