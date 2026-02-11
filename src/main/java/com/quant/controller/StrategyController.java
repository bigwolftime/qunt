package com.quant.controller;

import com.quant.dto.CreateStrategyRequest;
import com.quant.dto.StrategyResponse;
import com.quant.service.StrategyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @GetMapping
    public List<StrategyResponse> list() {
        return strategyService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StrategyResponse create(@Valid @RequestBody CreateStrategyRequest request) {
        return strategyService.create(request);
    }
}
