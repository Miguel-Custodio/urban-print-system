package com.urbanprint.backend.controller;

import com.urbanprint.backend.service.CustomPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final CustomPricingService service;

    public PricingController(CustomPricingService service) {
        this.service = service;
    }

    @PostMapping("/custom-calc")
    public ResponseEntity<?> calc(@RequestBody CustomPricingService.Request req) {
        try {
            return ResponseEntity.ok(service.calculate(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
