package com.urbanprint.backend.controller;

import com.urbanprint.backend.service.CutPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cut-plan")
public class CutPlanController {

    private final CutPlanService service;

    public CutPlanController(CutPlanService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> plan(@RequestParam double sheetW, @RequestParam double sheetH,
                                  @RequestParam double pieceW, @RequestParam double pieceH,
                                  @RequestParam(defaultValue = "1") int qty,
                                  @RequestParam(defaultValue = "0") double gap,
                                  @RequestParam(defaultValue = "0") double margin,
                                  @RequestParam(defaultValue = "true") boolean rotate) {
        try {
            return ResponseEntity.ok(service.plan(sheetW, sheetH, pieceW, pieceH, qty, gap, margin, rotate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}