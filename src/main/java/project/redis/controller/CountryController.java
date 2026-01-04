package project.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import project.redis.model.Country;
import project.redis.service.CountryService;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService service;

    @GetMapping("/{id}")
    public Country get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<Country> getAll() {
        return service.getAll();
    }

    @PostMapping
    public Country create(@RequestBody Country country) {
        return service.create(country);
    }

    @PutMapping
    public Country update(@RequestBody Country country) {
        return service.update(country);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/top")
    public List<Country> getTop(@RequestParam(defaultValue = "10") int limit) {
        return service.getTopPopular(limit);
    }
}
