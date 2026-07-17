package com.clientdesk.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {
}
