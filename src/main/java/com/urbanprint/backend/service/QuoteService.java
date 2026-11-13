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

        if (quote.getRushFeeType() == null) {
            quote.setRushFeeType("FIXED");
        }

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.setQuote(quote);
            }
        }

        calculateTotals(quote);

        return quoteRepository.save(quote);
    }

    @Transactional
    public Quote updateQuote(Long id, Quote updatedQuote) {
        return quoteRepository.findById(id).map(existing -> {

            existing.setCustomer(updatedQuote.getCustomer());
            existing.setIssueDate(updatedQuote.getIssueDate());
            existing.setExpiryDate(updatedQuote.getExpiryDate());
            existing.setStatus(updatedQuote.getStatus());
            existing.setTurnaroundOption(updatedQuote.getTurnaroundOption());
            existing.setNotes(updatedQuote.getNotes());
            existing.setGstRate(updatedQuote.getGstRate());
            existing.setPstRate(updatedQuote.getPstRate());
            existing.setDeliveryMethod(updatedQuote.getDeliveryMethod());

            // Rush editavel: tipo (FIXED/PERCENT), percentual e o valor fixo em $
            // (usado quando FIXED). O valor final e sempre recalculado em
            // calculateTotals -> applyRushFee.
            if (updatedQuote.getRushFeeType() != null) {
                existing.setRushFeeType(updatedQuote.getRushFeeType());
            }
            if (updatedQuote.getRushFeePercent() != null) {
                existing.setRushFeePercent(updatedQuote.getRushFeePercent());
            }
            if (updatedQuote.getRushFee() != null) {
                existing.setRushFee(updatedQuote.getRushFee());
            }

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

        // Rush fee editavel: valor fixo em $ ou % do subtotal (antes dos impostos)
        applyRushFee(quote, subtotal);

        BigDecimal rush = quote.getRushFee() != null ? quote.getRushFee() : BigDecimal.ZERO;

        // Base taxavel = subtotal + rush fee
        BigDecimal taxable = subtotal.add(rush);

        // GST
        BigDecimal gstAmount = taxable.multiply(quote.getGstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setGstAmount(gstAmount);

        // PST
        BigDecimal pstAmount = taxable.multiply(quote.getPstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setPstAmount(pstAmount);

        // Total = subtotal + rush + GST + PST
        quote.setTotalAmount(taxable.add(gstAmount).add(pstAmount));
    }

    // Rush fee so vale quando o turnaround e RUSH.
    // FIXED = valor fixo em $ (o que vier no campo; sem valor, usa o padrao de $20).
    // PERCENT = percentual do subtotal (base antes dos impostos).
    private void applyRushFee(Quote quote, BigDecimal subtotal) {
        if (!"RUSH".equals(quote.getTurnaroundOption())) {
            quote.setRushFee(BigDecimal.ZERO);
            if (quote.getRushFeeType() == null) {
                quote.setRushFeeType("FIXED");
            }
            return;
        }

        String type = quote.getRushFeeType() == null ? "FIXED" : quote.getRushFeeType();

        if ("PERCENT".equals(type)) {
            BigDecimal percent = quote.getRushFeePercent() == null
                    ? BigDecimal.ZERO
                    : quote.getRushFeePercent();

            quote.setRushFee(subtotal.multiply(percent)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        } else {
            quote.setRushFeeType("FIXED");
            quote.setRushFee(quote.getRushFee() == null
                    ? new BigDecimal("20.00")
                    : quote.getRushFee().max(BigDecimal.ZERO));
        }
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
                    // ignora quote numbers fora do padrao QU-XXXX
                }
            }
        }

        return String.format("QU-%04d", maxSeq + 1);
    }
}