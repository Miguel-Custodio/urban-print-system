package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    Optional<Quote> findByQuoteNumber(String quoteNumber);
    long count();
}