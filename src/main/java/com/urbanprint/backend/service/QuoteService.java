package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.model.QuoteItem;
import com.urbanprint.backend.repository.QuoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    public List<Quote> findAll() {
        return quoteRepository.findAll();
    }

    public Optional<Quote> findById(Long id) {
        return quoteRepository.findById(id);
    }

    public Quote save(Quote quote) {
        // Auto-generate quote number if missing
        if (quote.getQuoteNumber() == null || quote.getQuoteNumber().trim().isEmpty()) {
            long count = quoteRepository.count() + 1;
            quote.setQuoteNumber("QU-" + (1000 + count));
        }

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.setQuote(quote);
            }
        }
        quote.calculateTotals();
        return quoteRepository.save(quote);
    }

    public void deleteById(Long id) {
        quoteRepository.deleteById(id);
    }
}