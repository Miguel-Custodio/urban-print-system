package com.urbanprint.backend.controller;

import com.urbanprint.backend.service.CatalogImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/catalog-import")
public class CatalogImportController {

    private final CatalogImportService catalogImportService;

    public CatalogImportController(CatalogImportService catalogImportService) {
        this.catalogImportService = catalogImportService;
    }

    @PostMapping("/small-format")
    public Map<String, Object> importSmallFormat() {
        return catalogImportService.importSmallFormatCatalog();
    }
}