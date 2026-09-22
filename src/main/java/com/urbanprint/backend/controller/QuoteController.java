package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.model.QuoteItem;
import com.urbanprint.backend.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public List<Quote> findAll() {
        return quoteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> findById(@PathVariable Long id) {
        return quoteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Quote create(@RequestBody Quote quote) {
        return quoteService.save(quote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Quote> update(
            @PathVariable Long id,
            @RequestBody Quote quote
    ) {
        return quoteService.findById(id)
                .map(existingQuote -> {
                    existingQuote.setCustomer(quote.getCustomer());
                    existingQuote.setIssueDate(quote.getIssueDate());
                    existingQuote.setExpiryDate(quote.getExpiryDate());
                    existingQuote.setStatus(quote.getStatus());
                    existingQuote.setTaxRate(quote.getTaxRate());
                    existingQuote.setNotes(quote.getNotes());

                    existingQuote.getItems().clear();
                    if (quote.getItems() != null) {
                        for (QuoteItem item : quote.getItems()) {
                            existingQuote.addItem(item);
                        }
                    }

                    existingQuote.calculateTotals();
                    return ResponseEntity.ok(quoteService.save(existingQuote));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return quoteService.findById(id)
                .map(quote -> {
                    quoteService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}