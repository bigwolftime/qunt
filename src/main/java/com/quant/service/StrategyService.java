package com.quant.service;

import com.quant.domain.Strategy;
import com.quant.dto.CreateStrategyRequest;
import com.quant.dto.StrategyResponse;
import com.quant.repository.StrategyRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StrategyService {

    private final StrategyRepository strategyRepository;

    public StrategyService(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    @Cacheable(cacheNames = "strategies")
    public List<StrategyResponse> list() {
        return strategyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(cacheNames = "strategies", allEntries = true)
    public StrategyResponse create(CreateStrategyRequest request) {
        Strategy strategy = new Strategy();
        strategy.setName(request.name());
        strategy.setDescription(request.description());
        Strategy saved = strategyRepository.save(strategy);
        return toResponse(saved);
    }

    private StrategyResponse toResponse(Strategy strategy) {
        return new StrategyResponse(strategy.getId(), strategy.getName(), strategy.getDescription(), strategy.getCreatedAt());
    }
}
