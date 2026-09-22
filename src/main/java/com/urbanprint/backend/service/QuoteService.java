package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.model.QuoteItem;
import com.urbanprint.backend.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    public List<Quote> getAllQuotes() {
        return quoteRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Quote> getQuoteById(Long id) {
        return quoteRepository.findById(id);
    }

    @Transactional
    public Quote createQuote(Quote quote) {
        if (quote.getQuoteNumber() == null || quote.getQuoteNumber().trim().isEmpty()) {
            quote.setQuoteNumber(generateQuoteNumber());
        }

        if (quote.getIssueDate() == null) {
            quote.setIssueDate(LocalDate.now());
        }

        if (quote.getGstRate() == null) {
            quote.setGstRate(new BigDecimal("5.00"));
        }

        if (quote.getPstRate() == null) {
            quote.setPstRate(new BigDecimal("7.00"));
        }

        calculateTotals(quote);

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.setQuote(quote);
            }
        }

        return quoteRepository.save(quote);
    }

    @Transactional
    public Quote updateQuote(Long id, Quote updatedQuote) {
        return quoteRepository.findById(id).map(existing -> {
            existing.setCustomer(updatedQuote.getCustomer());
            existing.setIssueDate(updatedQuote.getIssueDate());
            existing.setExpiryDate(updatedQuote.getExpiryDate());
            existing.setStatus(updatedQuote.getStatus());
            existing.setNotes(updatedQuote.getNotes());
            existing.setGstRate(updatedQuote.getGstRate() != null ? updatedQuote.getGstRate() : new BigDecimal("5.00"));
            existing.setPstRate(updatedQuote.getPstRate() != null ? updatedQuote.getPstRate() : new BigDecimal("7.00"));

            existing.getItems().clear();
            if (updatedQuote.getItems() != null) {
                for (QuoteItem item : updatedQuote.getItems()) {
                    item.setQuote(existing);
                    existing.getItems().add(item);
                }
            }

            calculateTotals(existing);
            return quoteRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Quote not found with id " + id));
    }

    public void deleteQuote(Long id) {
        quoteRepository.deleteById(id);
    }

    private void calculateTotals(Quote quote) {
        BigDecimal subtotal = BigDecimal.ZERO;

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    BigDecimal itemTotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    item.setTotalPrice(itemTotal);
                    subtotal = subtotal.add(itemTotal);
                }
            }
        }

        quote.setSubtotal(subtotal);

        // GST (5%)
        BigDecimal gstAmount = subtotal.multiply(quote.getGstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setGstAmount(gstAmount);

        // PST (7%)
        BigDecimal pstAmount = subtotal.multiply(quote.getPstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setPstAmount(pstAmount);

        // Total = Subtotal + GST + PST
        quote.setTotalAmount(subtotal.add(gstAmount).add(pstAmount));
    }

    private String generateQuoteNumber() {
        long count = quoteRepository.count();
        return String.format("QU-%04d", count + 1001);
    }
}