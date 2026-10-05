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
        existing.setGstRate(updatedQuote.getGstRate());
        existing.setPstRate(updatedQuote.getPstRate());
        existing.setDeliveryMethod(updatedQuote.getDeliveryMethod());

        if (updatedQuote.getItems() == null) {
            updatedQuote.setItems(new java.util.ArrayList<>());
        }

        // 1. Remove items that were deleted on the edit screen
        java.util.Set<Long> keptIds = new java.util.HashSet<>();

        for (QuoteItem item : updatedQuote.getItems()) {
            if (item.getId() != null) {
                keptIds.add(item.getId());
            }
        }

        existing.getItems().removeIf(item ->
            item.getId() != null && !keptIds.contains(item.getId())
        );

        // 2. Update existing items and insert new ones
        for (QuoteItem newItem : updatedQuote.getItems()) {
            QuoteItem matched = newItem.getId() == null
                ? null
                : existing.getItems().stream()
                    .filter(oldItem -> oldItem.getId().equals(newItem.getId()))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                matched.setProduct(newItem.getProduct());
                matched.setItemDescription(newItem.getItemDescription());
                matched.setPrintSpecsSummary(newItem.getPrintSpecsSummary());
                matched.setQuantity(newItem.getQuantity());
                matched.setUnitPrice(newItem.getUnitPrice());
                matched.setTotalPrice(newItem.getTotalPrice());
            } else {
                newItem.setQuote(existing);
                existing.getItems().add(newItem);
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
        long maxSeq = 1000;

        for (Quote q : quoteRepository.findAll()) {
            String num = q.getQuoteNumber();

            if (num != null && num.startsWith("QU-")) {
                try {
                    long seq = Long.parseLong(num.substring(3));

                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                    // ignora quote numbers fora do padrão QU-XXXX
                }
            }
        }

        return String.format("QU-%04d", maxSeq + 1);
    }
}